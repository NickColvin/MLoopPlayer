(
s.latency.postln;
s.latency = 0.008;
s.makeGui;
s.options.blockSize = 64;
s.options.blockSize.postln;
o = s.options;
o.memSize = 65536;
o.memSize.postln;
o.hardwareBufferSize = 64;
o.hardwareBufferSize.postln;
s.boot;
)

s.quit



Server.internal.makeGui
s.makeGui

Server.default.postln
