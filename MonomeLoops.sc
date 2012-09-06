( // create monome and new event type

m = Monome.new("127.0.0.1", 16807);
Event.addEventType(\monomeLedEvent, { m.led(~col, ~row, ~i); {~dur.wait; m.led(~col, ~row, 0)}.fork; };);
p = Pbind(\type, \monomeLedEvent, \col, 0, \row, 0, \dur, 1, \i, 1);

)


(
m.button_func_user = {|msg|
	msg.postln;
	if(msg[3] == 1) {
		
		// m.led_row(0, msg[2], [0,0,0,0,0,0,0]);
		if(~bufArray[msg[2]].notNil) {
			~mLEDEvents[msg[2]].stop;
			~bufPlayers[msg[2]].stop;
		
		~mLEDEvents[msg[2]] = Pbindf(p, 
			\col, Pseq((0..7), inf, msg[1]),
			\row, msg[2],
			\i, 1,
			\dur, ~bufArray[msg[2]].numFrames/8/44100
		).play(quant: [0.125, 0, 0]);
		
		~bufPlayers[msg[2]] = Pmono(
			\MmLP,
			\out, 0,
			\trig, 1,
			\bufnum, ~bufArray[msg[2]],
			\startPos, Pseq((0..7), inf, msg[1])*(~bufArray[msg[2]].numFrames/8),
			\dur, ~bufArray[msg[2]].numFrames/8/44100
		).play(quant: [0.125, 0, 0]);
		
		}
		
		{"Buffer is nil".postln};
		
		
		
		
	}
}
)

~bufPlayers[0]

( // set up arrays
~bufArray = Array.fill(8, nil);
~mLEDEvents = Array.fill(8, nil);
~bufPlayers = Array.fill(8, nil);
)

( // fill arrays
~bufArray[0] = Buffer.read(s, "/Users/colvin/Documents/SC3/MLoopPlayer/Audio/MLRV.aif");
~bufArray[1] = Buffer.read(s, "/Users/colvin/Documents/SC3/MLoopPlayer/Audio/MLRVBass.aif");
~bufArray[2] = Buffer.read(s, "/Users/colvin/Documents/SC3/MLoopPlayer/Audio/MLRVPad.aif");
~bufArray[3] = Buffer.read(s, "/Users/colvin/Documents/SC3/MLoopPlayer/Audio/MLRVdrumghosts.aif");

)


( // sample player for Pmono
x = SynthDef(\MmLP, {|out = 0, bufnum, trig = 1, startPos = 0, rate = 1|
	Out.ar(out,
		PlayBuf.ar(2, bufnum, BufRateScale.kr(bufnum) * rate, trig, startPos, 1, 0)
		)
	}).add;
)

( //sample player for Ppoly
x = SynthDef(\MpLP, {|out = 0, bufnum, trig = 1, startPos = 0, gate = 1, fadeTime = 0.001|
	OffsetOut.ar(out,
		PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum), trig, startPos, 0,)
		* EnvGen.kr(Env.asr(fadeTime,1,fadeTime), gate, doneAction:2)
		)
	}).add;
)

