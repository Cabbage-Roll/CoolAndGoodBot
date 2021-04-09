const RIBBON_CLOSE_CODES = {
	'1000': 'ribbon closed normally',
	'1001': 'client closed ribbon',
	'1002': 'protocol error',
	'1003': 'protocol violation',
	'1006': 'ribbon lost',
	'1007': 'payload data corrupted',
	'1008': 'protocol violation',
	'1009': 'too much data',
	'1010': 'negotiation error',
	'1011': 'server error',
	'1012': 'server restarting',
	'1013': 'temporary error',
	'1014': 'bad gateway',
	'1015': 'TLS error'
};
const RIBBON_BATCH_TIMEOUT = 25;
const RIBBON_CACHE_EVICTION_TIME = 25000;

const RIBBON_EXTRACTED_ID_TAG = new Uint8Array([174]);
const RIBBON_STANDARD_ID_TAG = new Uint8Array([69]);
const RIBBON_BATCH_TAG = new Uint8Array([88]);
const RIBBON_EXTENSION_TAG = new Uint8Array([0xB0]);

const RIBBON_EXTENSIONS = new Map();
RIBBON_EXTENSIONS.set(0x0B, { command: 'ping' });
RIBBON_EXTENSIONS.set('PING', new Uint8Array([0xB0, 0x0B]));
RIBBON_EXTENSIONS.set(0x0C, { command: 'pong' });
RIBBON_EXTENSIONS.set('PONG', new Uint8Array([0xB0, 0x0C]));

// Ribbon - blazing-fast, msgpacked resumeable WebSockets 
const Ribbon = function(uri) {
	let endpoint = uri;
	let ws = null;
	let id = null;
	let resume = null;
	let lastSentId = 0;
	let lastReceivedId = 0;
	let lastSent = [];
	let lastSentTimes = [];
	let alive = true;
	let closeReason = 'ribbon lost';
	let messageListeners = {};
	let openListeners = [];
	let closeListeners = [];
	let pongListeners = [];
	let resumeListeners = [];
	let pingInterval = null;
	let lastPing = 0;
	let mayReconnect = true;
	let dead = false;
	let ignoreCleanness = false;
	let pingissues = false;
	let wasEverConnected = false;
	let ribbonSessionID = `SESS-${ Math.floor(Math.random() * Number.MAX_SAFE_INTEGER) }`;
	let incomingQueue = [];
	let switchingEndpoint = false;
	let batchQueue = [];
	let batchTimeout = null;
	let cacheSize = 200;
	let lastCacheReping = Date.now();

	pingInterval = setInterval(() => {
		if (!alive) {
			// we're pinging out, get our ass a new connection
			pingissues = true;
			console.warn('Ping timeout in ribbon. Abandoning current socket and obtaining a new one...');
			closeReason = 'ping timeout';
			Reconnect();
		}
		alive = false;
		if (ws && ws.readyState === 1) {
			lastPing = Date.now();
			try {
				if (ws.readyState === 1) {
					ws.send(SmartEncode('PING'));
				}
			} catch (ex) {}
		}

		// It's a bit cheeky to do this here, but it's fine. If we already have an interval, why create another one?
		while (lastSentTimes.length && (Date.now() - lastSentTimes[0]) >= RIBBON_CACHE_EVICTION_TIME) {
			lastSent.shift();
			lastSentTimes.shift();
		}
	}, 5000);

	function SmartEncode(packet) {
		if (typeof packet === 'string') {
			// This might be an extension, look it up
			const found = RIBBON_EXTENSIONS.get(packet);
			if (found) {
				return found;
			}
		}

		let prependable = RIBBON_STANDARD_ID_TAG;

		if (typeof packet === 'object' && packet.id && packet.command) {
			const id = packet.id;

			prependable = new Uint8Array(5);
			prependable.set(RIBBON_EXTRACTED_ID_TAG, 0);
			const view = new DataView(prependable.buffer);
			view.setUint32(1, id, false);
		}

		const msgpacked = msgpack.encode(packet);
		const merged = new Uint8Array(prependable.length + msgpacked.length);
		merged.set(prependable, 0);
		merged.set(msgpacked, prependable.length);

		return merged;
	}

	function SmartDecode(packet) {
		if (packet[0] === RIBBON_EXTENSION_TAG[0]) {
			// look up this extension
			const found = RIBBON_EXTENSIONS.get(packet[1]);
			if (!found) {
				console.error(`Unknown Ribbon extension ${ packet[1] }!`);
				console.error(packet);
				throw 'Unknown extension';
			}
			return found;
		} else if (packet[0] === RIBBON_STANDARD_ID_TAG[0]) {
			// simply extract
			return msgpack.decode(packet.slice(1));
		} else if (packet[0] === RIBBON_EXTRACTED_ID_TAG[0]) {
			// extract id and msgpacked, then inject id back in
			const object = msgpack.decode(packet.slice(5));
			const view = new DataView(packet.buffer);
			const id = view.getUint32(1, false);
			object.id = id;

			return object;
		} else if (packet[0] === RIBBON_BATCH_TAG[0]) {
			// ok these are complex, keep looking through the header until you get to the (uint32)0 delimiter
			const items = [];
			const lengths = [];
			const view = new DataView(packet.buffer);
			
			// Get the lengths
			for (let i = 0; true; i++) {
				const length = view.getUint32(1 + (i * 4), false);
				if (length === 0) {
					// We've hit the end of the batch
					break;
				}
				lengths.push(length);
			}

			// Get the items at those lengths
			let pointer = 0;
			for (let i = 0; i < lengths.length; i++) {
				items.push(packet.slice(1 + (lengths.length * 4) + 4 + pointer, 1 + (lengths.length * 4) + 4 + pointer + lengths[i]));
				pointer += lengths[i];
			}

			return { command: 'X-MUL', items: items.map(o => SmartDecode(o)) };
		} else {
			// try just parsing it straight?
			return msgpack.decode(packet);
		}
	}

	function Open() {
		if (ws) {
			// Discard the old socket entirely
			ws.onopen = ()=>{};
			ws.onmessage = ()=>{};
			ws.onerror = ()=>{};
			ws.onclose = ()=>{};
			ws.close();
		}

		ws = new WebSocket(endpoint, ribbonSessionID);
		incomingQueue = [];
		ws.onclose = (e) => {
			ignoreCleanness = false;
			if (RIBBON_CLOSE_CODES[e.code]) {
				closeReason = RIBBON_CLOSE_CODES[e.code];
			}
			if (closeReason === 'ribbon lost' && pingissues) {
				closeReason = 'ping timeout';
			}
			if (closeReason === 'ribbon lost' && !wasEverConnected) {
				closeReason = 'failed to connect';
			}
			console.log(`Ribbon ${ id } closed (${ closeReason })`);
			Reconnect();
		};
		ws.onopen = (e) => {
			wasEverConnected = true;
			if (resume) {
				console.log(`Ribbon ${ id } resuming`);
				ws.send(SmartEncode({ command: 'resume', socketid: id, resumetoken: resume }));
				ws.send(SmartEncode({ command: 'hello', packets: lastSent }));
			} else {
				ws.send(SmartEncode({ command: 'new' }));
			}
		};
		ws.onmessage = (e) => {
			try {
				new Response(e.data).arrayBuffer().then((ab) => {
					const msg = SmartDecode(new Uint8Array(ab));

					if (msg.command === 'kick') {
						mayReconnect = false;
					}

					if (msg.command === 'nope') {
						console.error(`Ribbon ${ id } noped out (${ msg.reason })`);
						mayReconnect = false;
						closeReason = msg.reason;
						Close();
					} else if (msg.command === 'hello') {
						id = msg.id;
						console.log(`Ribbon ${ id } ${ resume ? 'resumed' : 'opened' }`);
						resume = msg.resume;
						alive = true;
						msg.packets.forEach((p) => {
							HandleMessage(p);
						});
						openListeners.forEach((l) => {
							l();
						});
					} else if (msg.command === 'pong') {
						alive = true;
						pingissues = false;
						pongListeners.forEach((l) => {
							l(Date.now() - lastPing);
						});
					} else if (msg.command === 'X-MUL') {
						msg.items.forEach((m) => {
							HandleMessage(m);
						});
					} else {
						HandleMessage(msg);
					}
				});
			} catch (ex) {
				console.error('Failed to parse message', ex);
			}
		};
		ws.onerror = (e) => {
			if (!wasEverConnected) {
				if (messageListeners['connect_error']) {
					messageListeners['connect_error'].forEach((l) => {
						l();
					});
				}
			}
			console.log(e);
		};
		alive = true;
	}

	function HandleMessage(msg) {
		if (msg.id) {
			if (msg.id <= lastReceivedId) {
				return; // already seen this
			}

			EnqueueMessage(msg);
			return;
		}

		if (messageListeners[msg.command]) {
			messageListeners[msg.command].forEach((l) => {
				l(msg.data);
			});
		}
	}

	function EnqueueMessage(msg) {
		if (msg.id === lastReceivedId + 1) {
			// we're in order, all good!
			if (messageListeners[msg.command]) {
				messageListeners[msg.command].forEach((l) => {
					l(msg.data);
				});
			}
			lastReceivedId = msg.id;
		} else {
			incomingQueue.push(msg);
		}

		if (incomingQueue.length) {
			// Try to go through these
			incomingQueue.sort((a, b) => {
				return a.id - b.id;
			});

			while (incomingQueue.length) {
				const trackbackMessage = incomingQueue[0];

				if (trackbackMessage.id !== lastReceivedId + 1) {
					// no good, wait longer
					break;
				}

				// cool, let's push it
				incomingQueue.shift();
				if (messageListeners[trackbackMessage.command]) {
					messageListeners[trackbackMessage.command].forEach((l) => {
						l(trackbackMessage.data);
					});
				}
				lastReceivedId = trackbackMessage.id;
			}
		}
		if (incomingQueue.length > 5200) {
			console.error(`Ribbon ${ id } unrecoverable: ${ incomingQueue.length } packets out of order`);
			closeReason = 'too many lost packets';
			Close();
			return;
		}
	}

	function Send(command, data, batched = false) {
		const packet = { id: ++lastSentId, command, data };
		lastSent.push(packet);
		lastSentTimes.push(Date.now());

		if ((lastSentId % 100) === 0) {
			// recalculate how large our cache should be
			const packetsPerSecond = 1000 / ((Date.now() - lastCacheReping) / 100);
			cacheSize = Math.max(100, Math.min(30 * packetsPerSecond, 2000));
			lastCacheReping = Date.now();
		}

		while (lastSent.length > cacheSize) {
			lastSent.shift();
			lastSentTimes.shift();
		}

		while (lastSentTimes.length && (Date.now() - lastSentTimes[0]) >= RIBBON_CACHE_EVICTION_TIME) {
			lastSent.shift();
			lastSentTimes.shift();
		}

		if (batched) {
			batchQueue.push(SmartEncode(packet));

			if (!batchTimeout) {
				batchTimeout = setTimeout(FlushBatch, RIBBON_BATCH_TIMEOUT);
			}
			return;
		} else {
			FlushBatch();
		}

		try {
			if (ws.readyState === 1) {
				ws.send(SmartEncode(packet));
			}
		} catch (ex) {}
	}

	function FlushBatch() {
		if (!batchQueue.length) { return; }

		if (batchTimeout) {
			clearTimeout(batchTimeout);
			batchTimeout = null;
		}

		// If our batch is only 1 long we really dont need to go through this painful process
		if (batchQueue.length === 1) {
			try {
				if (ws.readyState === 1) {
					ws.send(batchQueue[0]);
				}
			} catch (ex) {}

			batchQueue = [];
			return;
		}

		// Get the total size of our payload, so we can prepare a buffer for it
		let totalSize = batchQueue.reduce((a, c) => { return a + c.length; }, 0);
		const buffer = new Uint8Array(1 + (batchQueue.length * 4) + 4 + totalSize);
		const view = new DataView(buffer.buffer);

		// Set the tag
		buffer.set(RIBBON_BATCH_TAG, 0);

		// Set the lengths and data blocks
		let pointer = 0;

		for (let i = 0; i < batchQueue.length; i++) {
			// Set the length
			view.setUint32(1 + (i * 4), batchQueue[i].length, false);

			// Set the data
			buffer.set(batchQueue[i], 1 + (batchQueue.length * 4) + 4 + pointer);
			pointer += batchQueue[i].length;
		}

		// Batch ready to send!
		try {
			if (ws.readyState === 1) {
				ws.send(buffer);
			}
		} catch (ex) {}

		batchQueue = [];
	}

	function Close(reason = null, silent = false) {
		mayReconnect = false;
		if (reason) {
			closeReason = reason;
		}
		if (ws) {
			ws.onclose = ()=>{};
			try {
				if (ws.readyState === 1) {
					ws.send(SmartEncode({ command: 'die' }));
				}
				ws.close();
			} catch (ex) {}
		}

		Die(silent);
	}

	function Cut(penalty = 0) {
		ignoreCleanness = true;
		extraPenalty = penalty;
		if (ws) {
			ws.close();
		}
	}

	function SwitchEndpoint() {
		console.warn(`Ribbon ${ id } changing endpoint (new endpoint: ${ endpoint })`);
		ignoreCleanness = true;
		switchingEndpoint = true;
		if (ws) {
			ws.close();
		}
	}

	let reconnectCount = 0;
	let lastReconnect = 0;
	let extraPenalty = 0;

	function Reconnect() {
		if (!switchingEndpoint) {
			if ((Date.now() - lastReconnect) > 40000) {
				reconnectCount = 0;
			}
			lastReconnect = Date.now();

			if (reconnectCount >= 10 || !mayReconnect) {
				// Stop bothering
				console.error(`Ribbon ${ id } abandoned: ${ mayReconnect ? 'too many reconnects' : 'may not reconnect' }`);
				Die();
				return;
			}

			console.warn(`Ribbon ${ id } reconnecting in ${ extraPenalty + 5 + 100 * reconnectCount }ms (reconnects: ${ reconnectCount + 1 })`);
			resumeListeners.forEach((l) => {
				l(extraPenalty + 5 + 100 * reconnectCount);
			});
		}
		
		setTimeout(() => {
			if (dead) {
				console.log(`Canceling reopen of ${ id }: no longer needed`);
				return;
			}

			Open();
		}, switchingEndpoint ? 0 : (extraPenalty + 5 + 100 * reconnectCount));

		if (switchingEndpoint) {
			switchingEndpoint = false;
		} else {
			reconnectCount++;
			extraPenalty = 0;
		}
	}

	function Die(silent = false) {
		if (dead) { return; }
		console.log(`Ribbon ${ id } dead (${ closeReason })`);
		dead = true;
		mayReconnect = false;
		if (!silent) {
			closeListeners.forEach((l) => {
				l(closeReason);
			});
		}
		ws.onopen = ()=>{};
		ws.onmessage = ()=>{};
		ws.onerror = ()=>{};
		ws.onclose = ()=>{};
		clearInterval(pingInterval);
	}


	// Publics
	return {
		getEndpoint: () => {
			return endpoint;
		},
		setEndpoint: (uri) => {
			endpoint = uri;
		},
		getId: () => { return id; },
		open: Open,
		isAlive: () => { return alive; },
		close: Close,
		send: Send,
		emit: Send,
		onclose: (l) => { closeListeners.push(l); },
		onopen: (l) => { openListeners.push(l); },
		onpong: (l) => { pongListeners.push(l); },
		onresume: (l) => { resumeListeners.push(l); },
		on: (type, l) => {
			if (messageListeners[type]) {
				messageListeners[type].push(l);
			} else {
				messageListeners[type] = [l];
			}
		},
		off: (type, l) => {
			if (messageListeners[type]) {
				if (!l) {
					messageListeners[type] = [];
					return;
				} else {
					messageListeners[type] = messageListeners[type].filter(o => o !== l);
				}
			}
		},
		cut: Cut,
		switchEndpoint: SwitchEndpoint
	};
}