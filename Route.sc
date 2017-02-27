Route{

	classvar windowIndex = 0;
	var count = 0, <>syn, buss, synthSize, synthNameList, numChan, <>amps, <>ampArraySize = 1, cmdperiod= nil, recordFlag = nil, recordSyn, recordBuf, synthDefineRandomNums, synthDefineRandomNumsIn, scopeBuffer, inputFlag = nil, inputBus;
	var <>recordPath = "/Works_S/Sounds/RouteRecDefault.wav", <>silentCutMode = false;
	var window, scopeView, scopePassSyns;
	var finalOut;
	var windowXmul = 0;
	var oscdefList, oscDefnames, <oscDefnamesCurrentNum, oscValidOrNot = 0, oscRandNums, oscSignalSndNums, <>ampfollowerAtkTime = 0.01, <>ampfollowerRelTime = 0.01, <>oscAutoNetPort = 7400, <>oscAutoNetAddress = "127.0.0.1", <>oscAutoOnOff = nil, oscSignalSndRandNum, oscSignalSndList, oscSignalSndImpulseRate = 60;
	var <>winOpen = true;


*new { |numchannels = 1|
		^super.new.init(numchannels);
	}


init { |numchannels|
		var s = Server.local;
		buss = List.newClear(0);
		syn = List.newClear(0);
		amps = List.newClear(0);
		synthNameList = List.newClear(0);
		synthSize = 0;
		numChan = numchannels;

		if(windowIndex > 2, {windowXmul = 1}); if(windowIndex > 5, {windowXmul = 2});
		window = Window("Route Scope "++windowIndex.asString, Rect(40+((windowXmul%3)*530),      Window.screenBounds.height-900+((windowIndex%3)*270),530,270));
		windowIndex = windowIndex + 1;
		window.view.background_(Color.new255(30,30,30));
		window.addFlowLayout(10@10, 10@10);
		scopeView = List.newClear(0);
		scopePassSyns = List.newClear(0);
		scopeBuffer = List.newClear(0);

		buss.add(Bus.audio(s,  numchannels));

		oscDefnames = List.newClear(0);
		oscDefnamesCurrentNum = 0;
		oscdefList = List.newClear(0);

		CmdPeriod.doOnce {
			buss.size.do{|i|
				buss[i].free;
				buss.removeAt(0);
		};

			buss = List.newClear(0);
			syn = List.newClear(0);
			synthNameList = List.newClear(0);
			synthSize = 0;

			buss.add(Bus.audio(s,  numchannels));

			cmdperiod = true;

};

		oscSignalSndNums = 0;
		oscSignalSndList = List.newClear(0);

		oscSignalSndRandNum = 10000.rand;

		synthDefineRandomNums = 10000.rand.asString;
		/////record synths

		SynthDef("Route_Record_" ++ synthDefineRandomNums, { arg buffer, buss, gate = 1;
			var inputSig = In.ar(buss, numChan);
			DiskOut.ar(buffer, EnvGen.ar(Env.new([0,1], [0.08]), gate)*inputSig);
			if(silentCutMode==true, {DetectSilence.ar(inputSig, time:0.1, doneAction: 2)});
		}).send(s);

		SynthDef("Route_scopePass_"++ synthDefineRandomNums, {arg bus, bufnum;
			var sig;
			sig = In.ar(bus, numChan);
			ScopeOut2.ar(sig, bufnum);
		}).send(s);

		SynthDef("Route_playBus_"++ synthDefineRandomNums, {arg bus, out, gate = 1;
			var sig;
			sig = EnvGen.ar(Env.new([0,1], [0.08]), gate)* In.ar(bus, numChan);
			Out.ar(out, sig);
		}).send(s);

		synthDefineRandomNumsIn = 10000.rand.asString;

		SynthDef("Route_InputPass_"++ synthDefineRandomNumsIn, {arg in, out, gate = 1;
			var sig;
			sig = EnvGen.ar(Env.new([0,1], [0.08]), gate)* In.ar(in, numChan);
			Out.ar(out, sig);
		}).send(s);

		SynthDef("Route_signalOscSend_", {arg in = 0;
		var snd;

		snd = SendReply.kr(Impulse.kr(oscSignalSndImpulseRate), '/SendReply'++ oscSignalSndRandNum.asSymbol, Amplitude.ar(In.ar(in, numChan), ampfollowerAtkTime, ampfollowerRelTime));
			}).send(s);

		////

		SynthDef(\rNoise,{
			arg out, amp = 0.5;
			var sig;
			sig = PinkNoise.ar(amp);
			Out.ar(out, sig);
		}).send(s);

		SynthDef(\rFilter,{
			arg in, out, amp = 1, cut = 400;
			var sig;
			sig = LPF.ar(In.ar(in, 1), cut);
			Out.ar(out, sig * amp);
		}).send(s);
}


rout{|synthName|

		var s = Server.local;

		synthNameList.add(synthName);
		if(amps.size <= synthSize, {amps.add(1.0)});
		if(amps.size > (synthSize+1), { amps=amps.lace(ampArraySize) });

		(syn.size).do{ |i|
			syn[i] !? {syn[i].free; syn[i] = nil;}
		};

		syn = List.newClear(0);

		(synthSize+1).do{|index|
			if(index == 0, {
			inputFlag !? {
			syn.add(Synth.tail(s, synthNameList[index], args: [\in, inputBus, \out, buss[index], \amp, amps[index]]));
			} ?? {
			syn.add(Synth.tail(s, synthNameList[index], args: [\in, buss[index-1], \out, buss[index], \amp, amps[index]]));
			}
			}, {
			syn.add(Synth.tail(s, synthNameList[index], args: [\in, buss[index-1], \out, buss[index], \amp, amps[index]]));
			});

		};

		oscAutoOnOff !? { this.oscSignalSnd(buss[synthSize], oscAutoNetPort, oscAutoNetAddress, synthDefineRandomNums.asSymbol++'_auto_')};

		this.addScopeView(synthSize);

		synthSize = synthSize + 1;
		buss.add(Bus.audio(s,  numChan));

}


getOscSendAddress{

		^"/SC_Route_"++synthDefineRandomNums++" "++" and _auto_ or number Indicated!"

	}


oscRcv { |synArrayNum, synParamName, msgOrNot = nil|

		oscDefnames.add(synthDefineRandomNums.asString ++"_"++ synArrayNum++"_"++oscDefnamesCurrentNum);


		oscdefList.add(OSCdef.newMatching(oscDefnames[oscDefnamesCurrentNum],
			{|msg, time, addr, recvPort|

				msgOrNot !? {msg[1].postln};

				syn[synArrayNum].set(synParamName, msg[1]);

		}, synArrayNum.asString++"_"++synParamName.asString));



		oscDefnamesCurrentNum = oscDefnamesCurrentNum+1;

		cmdperiod !? {CmdPeriod.doOnce {
			cmdperiod = true;

			oscdefList.size.do{|i|
				oscdefList[i].free;
			};


			oscDefnames = List.newClear(0);  //////////////////////
		}
		};
}



oscSignalSnd{|busInput, netPort, netAddress = "127.0.0.1", prefix = 0|
		var s = Server.local;
		var localNet = NetAddr("localhost", NetAddr.langPort);
		var sendNet = NetAddr(netAddress, netPort);


		OSCdef(\oscSignalSndOSCdef_++synthDefineRandomNums.asSymbol++(oscSignalSndNums-1).asSymbol).free;

		Synth.after(s,"Route_signalOscSend_", args: [\in, busInput]);

		oscSignalSndList.add(0);

		(oscSignalSndNums+1).do{|index|
		var temp;
			// oscSignalSndNums가 모종의 이유로 1부터 시작?
			temp = oscSignalSndRandNum-index;
			oscdefList.add(OSCdef.newMatching(\oscSignalTakeOSCdef_++oscSignalSndRandNum.asSymbol, {|msg|  oscSignalSndList.put(index, msg[3].round(0.000001))}, '/SendReply'++ temp.asSymbol));

		};


		oscdefList.add(OSCdef.newMatching(\oscSignalSndOSCdef_++synthDefineRandomNums.asSymbol++oscSignalSndNums.asSymbol, {
			sendNet.sendMsg('/SC_Route_'++prefix.asSymbol, *(oscSignalSndList.asArray))
		}, '/SendReply'++ oscSignalSndRandNum.asSymbol));


		oscSignalSndNums = oscSignalSndNums+1;
		oscSignalSndRandNum = oscSignalSndRandNum+1;

		SynthDef("Route_signalOscSend_", {arg in = 0;
			var snd;
			snd = SendReply.kr(Impulse.kr(oscSignalSndImpulseRate), '/SendReply'++ oscSignalSndRandNum.asSymbol, Amplitude.ar(In.ar(in, numChan), ampfollowerAtkTime, ampfollowerRelTime));
			}).send(s);


		cmdperiod !? {CmdPeriod.doOnce {
			cmdperiod = true;
			oscdefList.size.do{|i|
				oscdefList[i].free;
			};
		}
		};

}



>> {|busInput|
		var s = Server.local;

		this.cut;

		{

		inputBus = busInput;

		synthNameList.add("Route_InputPass_"++ synthDefineRandomNumsIn);
		if(amps.size <= synthSize, {amps.add(1.0)});
		if(amps.size > (synthSize+1), { amps=amps.lace(ampArraySize) });

		(syn.size).do{ |i|
			syn[i] !? {syn[i].free; syn[i] = nil;}
		};

		syn = List.newClear(0);


		syn.add(Synth.tail(s, synthNameList[0], args: [\in, inputBus, \out, buss[0]]));

		synthSize = synthSize + 1;
		buss.add(Bus.audio(s,  numChan));

		inputFlag = true;

		}.defer;


	}


addScopeView{|index|
		var s = Server.local, busIndex;

		inputFlag !? {index = index-1};

		scopeBuffer.add(Buffer.alloc(s, 1024, numChan));

		scopeView.add(ScopeView(window.view, 120@120));

		inputFlag !? {busIndex = index +1} ?? {busIndex = index};

		scopePassSyns.add(Synth.after(s, "Route_scopePass_"++ synthDefineRandomNums, [\bus, buss[busIndex], \bufnum, scopeBuffer[index]]));

		scopeView[index].server = s;
		scopeView[index].bufnum = scopeBuffer[index];
		scopeView[index].start;

		recordFlag !? { 	scopeView[index].backColor = Color.new255(15,0,0); };

}



cut{

		var s = Server.local;

		inputFlag !? {inputBus = Bus.audio(s, numChan)};

		cmdperiod ?? {
			buss.size.do{|i|
			buss[i].free;
			buss.removeAt(0);
		};

		syn.size.do{ |i|
			syn[i] !? {syn[i].free;}
		};

			buss = List.newClear(0);
			syn = List.newClear(0);
			synthNameList = List.newClear(0);
			synthSize = 0;

		buss.add(Bus.audio(s,  numChan));

		scopeView.size.do{ |i|
			if(window.isClosed == false,{
				scopeView[i].close;
				scopePassSyns[i].free;
			});
		};

		inputFlag !? inputBus.free;

		scopeBuffer.size.do{ |i|
				scopeBuffer[i].freeMsg;
			};

		scopeBuffer = List.newClear(0);
		recordFlag !? recordSyn.free;

		recordFlag !? {
		recordBuf.close;
		recordBuf.freeMsg;
			};
		recordFlag = nil;

		finalOut.free;


		oscdefList.size.do{|i|
				oscdefList[i].free;
		};

		oscDefnames = List.newClear(0);

		oscSignalSndList = List.newClear(0);
		oscSignalSndNums = 0;

		oscdefList = List.newClear(0);

		SynthDef("Route_signalOscSend_", {arg in = 0;
			var snd;
			snd = SendReply.kr(Impulse.kr(oscSignalSndImpulseRate), '/SendReply'++ oscSignalSndRandNum.asSymbol, Amplitude.ar(In.ar(in, numChan), ampfollowerAtkTime, ampfollowerRelTime));
			}).send(s);

		};


		cmdperiod !? {CmdPeriod.doOnce {
			cmdperiod = true;

			buss.size.do{|i|
				buss[i].free;
				buss.removeAt(0);
				};
			};

			buss = List.newClear(0);
			syn = List.newClear(0);
			synthNameList = List.newClear(0);
			synthSize = 0;

			scopeBuffer.size.do{ |i|
				scopeBuffer[i].freeMsg;
			};

		    scopeBuffer = List.newClear(0);

		    inputFlag !? inputBus.free;

			buss.add(Bus.audio(s,  numChan));

		    oscSignalSndList = List.newClear(0);
		    oscSignalSndNums = 0;
		};


		scopeView = List.newClear(0);
		scopePassSyns = List.newClear(0);

		if(window.isClosed == false,{
		window.view.decorator.reset;
		if(winOpen == true, {
			if(cmdperiod == nil, {
				window.front;
			});
		});
	});

		oscSignalSndList = List.newClear(0);
		oscSignalSndNums = 0;
		oscdefList = List.newClear(0);

		cmdperiod = nil;
}


|> { |synthName|
		{
		this.cut;

		this.rout(synthName);
		}.defer;

}


=>{ |synthName|

		{
		this.rout(synthName);
		}.defer;

}



=+{|synthName|
		var s = Server.local;

		{
	synthNameList.add(synthName);
		if(amps.size <= synthSize, {amps.add(1.0)});
		if(amps.size > (synthSize+1), { amps=amps.lace(ampArraySize) });

		(syn.size).do{ |i|
			syn[i] !? {syn[i].free; syn[i] = nil;}
		};

		syn = List.newClear(0);

		(synthSize+1).do{|index|
			if(index == 0, {
			inputFlag !? {
			syn.add(Synth.tail(s, synthNameList[index], args: [\in, inputBus, \out, buss[index], \amp, amps[index]]));
			} ?? {
			syn.add(Synth.tail(s, synthNameList[index], args: [\in, buss[index-1], \out, buss[index], \amp, amps[index]]));
			}
			}, {
			syn.add(Synth.tail(s, synthNameList[index], args: [\in, buss[index-1], \out, buss[index], \amp, amps[index]]));
			});

		};


		recordBuf = Buffer.alloc(s, 65536, numChan);
		recordBuf.write(recordPath, "wav", "int24", 0, 0, true);

		recordSyn = Synth.tail(s, "Route_Record_"++ synthDefineRandomNums, [\buffer, recordBuf, \buss, buss[synthSize]]);
		recordFlag = true;


		cmdperiod !? CmdPeriod.doOnce {
					recordBuf.close;
					recordBuf.freeMsg;
					recordFlag = nil;
		};


		this.addScopeView(synthSize);
		synthSize = synthSize + 1;

		}.defer;

}



=={|outChannel|
		var s = Server.local;
		{
		synthSize = synthSize -1;
		finalOut = Synth.tail(s, "Route_playBus_"++ synthDefineRandomNums, [\bus, buss[synthSize], \out, outChannel]);
		}.defer;
}


}