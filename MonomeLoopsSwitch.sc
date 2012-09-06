( // Init: create monome, monome LED event type, arrays etc.

m = Monome.new("127.0.0.1", 16807);
Event.addEventType(\monomeLedEvent, 
	{ 
		m.led(~col, ~row, ~i);
			{
				~dur.wait; 
				m.led(~col, ~row, 0);
			}.fork; 
	};
	);
p = Pbind(\type, \monomeLedEvent, \col, 0, \row, 0, \dur, 1, \i, 1, \finish, {m.led(~col, ~row, 0)}); // default monome led event
q = [0.125, 0, 0]; // store global quant values
t = TempoClock.tempo = 100/60;

// init arrays
~bufArray = Array.fill(8, nil);
~mLEDEvents = Array.fill(8, nil);
~bufPlayers = Array.fill(8, nil);


)



(
m.button_func_user = {|msg|
	
	switch(msg[2],
		7, {
			switch(msg[1],
				7, { ~mLEDEvents.do(_.stop); ~bufPlayers.do(_.stop);},
				{~mLEDEvents[msg[1]].stop; ~bufPlayers[msg[1]].stop;}
				)
			}
		, 
	
	{ if(msg[3] == 1) {
		
		if(~bufArray[msg[2]][0].notNil) {
			var bufnum, bufLength, beatLength, bufBeats;
			
			bufLength = ~bufArray[msg[2]][0].numFrames/44100;
			beatLength = ~bufArray[msg[2]][0].numFrames/44100/8;
			bufBeats = ~bufArray[msg[2]][1];
			bufnum = ~bufArray[msg[2]][0];		
			
			~mLEDEvents[msg[2]].stop;
			~bufPlayers[msg[2]].stop;
		
		~mLEDEvents[msg[2]] = Pbindf(p, 
			\col, Pseq((0..7), inf, msg[1]),
			\row, msg[2],
			\i, 1,
			\dur, bufBeats / m.grid.size,
/*			\dur, Pfunc( {(t.tempo * ~bufArray[msg[2]].numFrames/44100/8)} ), // This maintains original tempo of sample regardless of underlying temp*/
		).play(t, quant: q);
		
		
		// Use Pmono style
/*		~bufPlayers[msg[2]] = Pmono(
			\MmLP,
			\out, 0,
			\trig, 1,
			\bufnum, ~bufArray[msg[2]],
			\startPos, Pseq((0..7), inf, msg[1])*(~bufArray[msg[2]].numFrames/8),
			\dur, 2 //~bufArray[msg[2]].numFrames/8/44100
		).play(quant: q);
*/		
		
		// Use poly
/*		~bufPlayers[msg[2]] = Pbind(
			\instrument, \MpLP,
			\out, 0,
			\trig, 1,
			\bufnum, ~bufArray[msg[2]][0],
			\startPos, Pseq((0..7), inf, msg[1])*(~bufArray[msg[2]][0].numFrames/8),
		//	\dur, Pfunc( {(t.tempo * ~bufArray[msg[2]].numFrames/44100/8)} ), // This maintains original tempo of sample regardless of underlying temp
			\dur, bufBeats / m.grid.size,
			\rate, Pfunc({t.tempo * ~bufArray[msg[2]][0].numFrames/bufBeats/44100}),
			\legato, 1
		).play(t, quant: q);*/

		// Use warp
		~bufPlayers[msg[2]] = Pbind(
			\instrument, \Mwarp,
			\out, 0,
			\trig, 1,
			\bufnum, bufnum,
			\beatLength, beatLength,
			\startPos, Pseq((0..7), inf, msg[1]),
		//	\dur, Pfunc( {(t.tempo * ~bufArray[msg[2]].numFrames/44100/8)} ), // This maintains original tempo of sample regardless of underlying temp
			\dur, bufBeats / m.grid.size,
			\length, bufLength,
		//	\pitch, 2,
			\rate, Pfunc({t.tempo * bufnum.numFrames/4/44100}),
			\legato, 1
		).play(t, quant: q);
		
		}
		
		{"Buffer is nil".postln};
	}
	
		
		
	}
	)
}
)



( // fill buffers array
~bufArray[0] = [Buffer.read(s, "/Users/colvin/Documents/SC3/MLoopPlayer/Audio/MLRV.aif"),4];
~bufArray[1] = [Buffer.read(s, "/Users/colvin/Documents/SC3/MLoopPlayer/Audio/MLRVBass.aif"),4];
~bufArray[2] = [Buffer.read(s, "/Users/colvin/Documents/SC3/MLoopPlayer/Audio/MLRVPad.aif"),16];
~bufArray[3] = [Buffer.read(s, "/Users/colvin/Documents/SC3/MLoopPlayer/Audio/MLRVdrumghosts.aif"),4];

)


( // sample player for Pmono
x = SynthDef(\MmLP, {|out = 0, bufnum, trig = 1, startPos = 0, rate = 1, gate = 1|
	
	var env, snd, fadeTime = 0.01;
	
	env = EnvGen.kr(Env.asr(fadeTime, 1, fadeTime, curve: \sqr), gate, doneAction: 2);
	
	snd = PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum) * rate, trig, startPos, 1, 0);
	snd = snd * env;
	Out.ar(out, snd);
	
/*	Out.ar(out,
		PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum) * rate, trig, startPos, 1, 0)
		)
*/	}).add;
)

( //sample player for Poly
y = SynthDef(\MpLP, {|out = 0, bufnum, trig = 1, startPos = 0, gate = 1, rate = 1, fadeTime = 0.01, dur|
	
	var snd, env;
	
	env = EnvGen.kr(Env.asr(fadeTime, 1, fadeTime, curve: \sqr), gate, doneAction:2);
	
	snd = PlayBuf.ar(2, bufnum, rate, trig, startPos, 0);
	snd = snd * env;
	
	OffsetOut.ar(out, snd);
	}).add;
)


( //sample player for warp
z = SynthDef(\Mwarp, {|out = 0, bufnum, beatLength, trig = 1, startPos = 0, gate = 1, pitch = 1, fadeTime = 0.01, dur|
	var snd, env, pointer, stepIndex;
	
	stepIndex = startPos*0.125;
	
	pointer = Line.kr(stepIndex, stepIndex + 0.125, beatLength);
	
	env = EnvGen.kr(Env.asr(fadeTime, 1, fadeTime, curve: \sqr), gate, doneAction:2);
	
	snd = Warp1.ar(2, bufnum, pointer, pitch, windowSize: 0.04, envbufnum: -1, overlaps: 6, windowRandRatio: 0.2, interp: 4);
	snd = snd * env;
	
	OffsetOut.ar(out, snd);
	
	}).add;
)