
function outvpi(lfc, tag, val){
   print RUN_ID"\t"iter"\t"lfc"\t"tag"\t"val >> vpidir;
} 


function outvpr(tag, val){
   print RUN_ID"\t"tag"\t"val >> vprdir;
}

function outputridx(){

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

  vpidir = ODIR"/ValuesPerIter.txt";
  vprdir = ODIR"/ValuesPerRun.txt";
  ridxdir = ODIR"/RunIndex.txt";

  iter = 0;
  FC = "N";
  succeed = 0;
}

/BEG FIND/{ 

  FC = "FIND";

}

/decisions/{
  outvpi(FC, "decisions", $4);
}

/clauses/{
  outvpi(FC, "clauses", $4);
}


/Slice interface fraction/{ outvpi("FIND", "interfrac", $5); }
/Slice fraction/{ outvpi("FIND", "slicefrac", $4); }


/successful.*true/{
  succeed = 1;

}

/BEG CHECK/{

  FC = "CHECK";
}

/ftime.*ctime/{

  outvpi("FIND", "TIME", $4);
  outvpi("CHECK", "TIME", $6);
  iter = iter + 1;

}

/optimization level/{

  outvpr("OLEVEL", $4);

}

/Total elapsed time/{

  outvpr("TOTIME", $5);

}

/Model building time \(ms\)/{

  outvpr("BUILDTIME", $5);
}



/Program in File/{ 

  outvpr("BENCHMARK", $6); 

}

/_find =/{

  outvpr("FSOLVER", $3);

}

/_check =/{

  outvpr("CSOLVER", $3);
}

/GOT THE CORRECT ANSWER/{

  outvpr("ITERS", $6);
}

/FIND TIME.*CHECK TIME/{
  
  outvpr("FTIME", $3);
  outvpr("CTIME", $6);

}

/INBITS.*=/{
# This is the value of the --inbits flag.
  outvpr("IBFLAG", $3);
  
}

/CBITS.*=/{
  
  outvpr("CBFLAG", $3);

}

/input_ints.*input_bits/{

  outvpr("INTINS", $3);
  outvpr("BITINS", $6);

}

/inputSize.*ctrlSize/{

  outvpr("TINBITS", $3);
  outvpr("TCBITS", $6);

}
/control_ints.*control_bits/{

  outvpr("INTCS", $3);
  outvpr("BITCS", $6);


}

/OF CONTROLS/{

  outvpr("TC", $4);

}



/Max virtual mem/{

  outvpr("MXVMEM", $5);

}

/Max resident mem/{

  outvpr("MRMEM", $5);

}


/Max private mem/{

  outvpr("MPMEM", $5);

}

/Random seeds =/{

  outvpr("RSEEDS", $4);

}




/SOLVER RAND SEED/{

  outvpr("SEED", $5);

}



/MAX LOOP UNROLLING/{

  outvpr("UNROLL", $6);
}



/MAX FUNC INL/{
 
  outvpr("INLINING", $6);
}


/after Creating Miter/{

  outvpr("MITERSIZE", $7);

}



/Final Problem size/{

  outvpr("FINALPSIZE", $8);

}


END{

  outputridx();

}








