
function outvpi(lfc, tag, val){
   print RUN_ID"\t"iter"\t"lfc"\t"tag"\t"val >> vpidir;
} 


function outvpr(tag, val){
   print RUN_ID"\t"tag"\t"val >> vprdir;
}

function outputridx(){
   if(timeout!=0){ succeed = -1; }
   print RUN_ID"\t"VERSION"\t"succeed >> ridxdir;
}

BEGIN{ 
#  Required Parameters: 
#  RUN_ID;
#  VERSION;
#  ODIR;
#  

  print v"ver";
  
  print "RUN_ID="RUN_ID "\t VERSION="VERSION"\t ODIR="ODIR

  vpidir = ODIR"/ValuesPerIter_par.txt";
  vprdir = ODIR"/ValuesPerRun_par.txt";
  ridxdir = ODIR"/RunIndex_par.txt";
  timeout=0;
  iter = -1;
  FC = "N";
  succeed = 0;
}

/SPINVERIF/{ 

  FC = "CHECK";

}

/SATBackend/{

  FC = "FIND";
}

/Compilation statistics/{
  FC = "FINAL";
}

/Synthesizer statistics/{
  FC = "FINALSYNTH";

}
/Verifier statistics/{
  FC = "FINALVERIF";

}
/Frontend statistics/{
  FC = "FINALFE";

}
/Solved.*true/{
  succeed = 1;

}


/Iteration/{

  iter = iter + 1;

}

/optimization level/{
  OLEVEL = $4;


}

/playDumb = YES/{
	  outvpr("DUMB", "YES");
}

/playDumb = NO/{
	  outvpr("DUMB", "NO");
}

/playDumb = RAND/{
	  outvpr("DUMB", "RAND");
}


/Total elapsed time \(s\)/{
  if(FC=="FINAL"){
	  outvpr("TOTIME", $6);
  }
  if(FC=="FINALSYNTH"){
	  outvpr("FTIME", $6);
  }
  if(FC=="FINALVERIF"){
	  outvpr("CTIME", $6);
  }
  if(FC=="FINALFE"){
	  outvpr("F_TIME", $6);
  }

}
/Total solution time \(s\)/{
  if(FC=="FIND" || FC=="CHECK"){
        outvpi(FC, "SOLTIME", $5);
  }
  if(FC=="FINAL"){
        outvpr("TOTALSOLTIME", $6);
  }
  if(FC=="FINALSYNTH"){
	  outvpr("S_SOLTIME", $6);
  }
  if(FC=="FINALVERIF"){
	  outvpr("V_SOLTIME", $6);
  }
}
/Average memory usage/{

  if(FC=="FINALSYNTH"){

	  outvpr("S_AVMEM", $6);
  }
  if(FC=="FINALVERIF"){

	  outvpr("V_AVMEM", $6);
  }
}

/Maximum memory usage \(MiB\)/{

  if(FC=="FINAL"){
	  outvpr("TOTMAXMEM", $6);
  }
  if(FC=="FINALSYNTH"){
	  outvpr("S_MAXMEM", $6);
  }
  if(FC=="FINALVERIF"){
	  outvpr("V_MAXMEM", $6);
  }
}

/compiler time \(s\)/{
  outvpi(FC, "COMPILETIME", $5);
}
/compiler memory/{
  outvpi(FC, "COMPILEMEM", $5);
}


/total SPIN time \(s\)/{
  outvpi(FC, "SPINTIME", $6);
}

/total SPIN mem \(MiB\)/{
  outvpi(FC, "SPINMEM", $6);
}


/states explored/{
  outvpi(FC, "STATES", $5);
}

/initial number of nodes/{
  outvpi(FC, "MITERSIZE", $6);
}


/number of nodes after opts/{
  outvpi(FC, "FINALPSIZE", $7);
}


/total number of control bits/{
  outvpi(FC, "TC", $7);
}

/number of controls/{
  outvpi(FC, "TCBITS", $5);
}




/ax memory usage \(MiB\)/{
  if(FC=="FIND" || FC=="CHECK"){
          outvpi(FC, "MAXMEM", $6);
  }  
  if(FC=="FINALFE"){
	  outvpr("F_MAXMEM", $6);
  }

}
/Total model building time \(s\)/{

  if(FC=="FIND" || FC=="CHECK"){
           outvpi(FC, "BUILDTIME", $6);
  }

  if(FC=="FINALSYNTH"){
           outvpr("S_BUILDTIME", $7);
  }

  if(FC=="FINALVERIF"){
           outvpr("V_BUILDTIME", $7);
  }
}
/elapsed time \(s\)/{
  if(FC=="FIND" || FC=="CHECK"){
           outvpi(FC, "TOTTIME", $5);
  }
}


/Benchmark =/{ 
  outvpr("BENCHMARK", $3); 
}



/_find =/{

  FSOLVER=$3;

}


/SOLVER RAND SEED/{

  SEED=$5;

}
/INBITS.*=/{
# This is the value of the --inbits flag.
  IBFLAG = $3;
  
}

/CBITS.*=/{
  CBFLAG= $3;
}

/MAX LOOP UNROLLING/{

  UNROLL= $6;
}

/MAX FUNC INL/{
 
  INLINING= $6;
}

/Time limit exceeded/{
  timeout=1;
}

END{
  outvpr("FSOLVER", FSOLVER);
  outvpr("SEED", SEED);
  outvpr("IBFLAG", IBFLAG);
  outvpr("CBFLAG", CBFLAG);
  
  outvpr("UNROLL", UNROLL);
  outvpr("INLINING", INLINING);
  outvpr("ITERS", iter);  
  outvpr("OLEVEL", OLEVEL);
  outputridx();

}








