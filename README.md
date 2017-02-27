# Route

 Symbolic synth routing class for Supercollider inspired by ChucK and Faust, with convenient oscilloscope window; recording; osc send&receive feature.
by Philip Liu

To use this class with synths arg 'out' must be decalred. 'in' also should be decalred from second synth because through the 'in' a synth takes signal from the previous one. put 'In.ar(in)' to desired place.
optionally, if you decalre 'amp',  by the method '.amps' you can set all the amp value of synths at once by one array


SynthDef(\cont, { arg out,amp= 6,scope, mfreq = 3;
	var sig, gate = 1;
	sig = SinOsc.ar(mfreq);
	Out.ar(out,  sig);
}).send(s);

SynthDef(\osc, { arg out, in, amp; Out.ar(out, amp*SinOsc.ar(1000*In.ar(in))); }).send(s);

SynthDef(\filt, { arg out, in, amp, cut=2000; Out.ar(out, amp*LPF.ar(In.ar(in), cut)); }).send(s);

SynthDef(\rvb, { arg out, in, amp, roomsize = 10 ; Out.ar(out, amp* 0.2*GVerb.ar(In.ar(in), roomsize)); }).send(s);


//decalre an audio bus

~bus = Bus.audio(s, 1);

//1 means the instance will be 1 channel

r = Route(1);

//2 means the instance will be 2 channel  .. 3..4 and so on.

f = Route(2);

//Route synth cont to osc to filter then out to channel 0. 

//Oscillscope window which shows all the synths in this instance is pop up when you actually start routing 

r|>\cont=>\osc=>\filt==0;

//Route synth cont to osc to filter then out to the audio bus

r|>\cont=>\osc=+\filt==~bus;

//f is 2 channel, so now the signal is 2 channel

f>>~bus=>\rvb==0;

r.syn[4].set(\roomsize,2);

r.syn[0].set(\mfreq, 0.01);

r.syn[1].set(\amp, 0.02);

r.amps = [1,1,1,0.2,0.2]

//if you put  =+ instead of => right before the last synth, output from the last synth will be recorded

r|>\cont=>\osc=+\filt==0;

r.recordPath_("/Works_S/Sounds/RouteRecDefault.wav");


//you can also send and receive osc messages easily. check each methods

