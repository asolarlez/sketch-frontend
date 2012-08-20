BEGIN{
        lnodes = 0;
        ptime = 0.0;
        betime = 0.0;
  stime = 0.0;
  lastSclauses = 0;
        i=1;
}
/f#.*clauses/{
  lastSclauses = $5;
}
/Benchmark = /{
  bmark = $3;
  superbmark = bmark;
  sub(".*/", "", bmark);
  sub("\\..*", "", bmark);
}
/Final Problem size: Problem nodes/{
   lnodes = lnodes + $8

}

/Time limit exceeded!/{
  TIMEOUT=1;
}

function reset(){
       ptime = 0.0;
       betime = 0.0;
       stime = 0.0;
       ctime = 0.0;
  lastSclauses=0;
    lnodes = 0;
    TIMEOUT=0;
       i=i+1;
}


/The sketch could not be resolved/{
        ar[i] = 2000;
        beoverhead[i] = 2000;
        ttimear[i] = 2000;
        verif[i] = 2000;
        synth[i] = 2000;
        nodes[i] = lnodes;

       synthClause[i] = lastSclauses;
       Benchmark[i] = bmark;
    reset();
}

/Total time =/{
       if(TIMEOUT==0){
        ar[i] = ptime;
        beoverhead[i] = betime-ptime;
        ttimear[i] = ($4/1000.0)-betime;
        synth[i] = stime;
        verif[i] = ctime;

        nodes[i] = lnodes;
       }else{
        ar[i] = 2000;
        beoverhead[i] = 2000;
        verif[i] = 2000;
        ttimear[i] = 2000;
        synth[i] = 2000;
        nodes[i] = lnodes;
       }

       synthClause[i] = lastSclauses;
       Benchmark[i] = bmark;
  reset();
}
/elapsed time \(s\)/{
  if($5>0 && $5!= ""){
    betime = betime + $5;
    }


}
/solution time /{
  if($5>0 && $5!= ""){
    ptime = ptime + $5;
    }
}
/FIND TIME/{
  tt = $3 / 1000.0;
  stime = stime + tt;
  tt = $6 / 1000.0;

  ctime = ctime + tt;
}

function printstats(a){
  BINS = 20;
  n=asort(a);
  kk=n/BINS;
  i=1;
  it=0;
  while(i<=n){
    it=it+1;
                cnt[it] = 0;
                tot[it] = 0.0;
                mn[it] = 0.0;
                mx[it] = 0.0;
    k = kk;
    if(it <= n % BINS){
      k = k + 1;
    }
    for(t=1; t<=k && i<=n; t++){
                        mx[it] = a[i];
                        if(t==1){ mn[it] = a[i]; }
      cnt[it] = cnt[it] + 1;
      tot[it] = tot[it] + a[i];
      i=i+1;
    }
  }
        for(k=BINS/2; k<=it; ++k){
    print" decil "k"  [ "mn[k]", "mx[k]" ] cnt= "cnt[k]"  avg= "(tot[k]/cnt[k]);

  }
}

function bmstat(bm, list){
  tt = 1;
  for(i in tlist){
    delete tlist[i];
  }
  for(i in Benchmark){
    if(bm == Benchmark[i]){
      tlist[tt] = list[i];
      tt = tt + 1;
    }
  }
  n=asort(tlist);
  low = tlist[1];
  high = tlist[n];
  med = tlist[(n + (n%2))/2];
  return low"  "med"  "high;
}

function detailed(){
  for(i in Benchmark){
    bmlist[Benchmark[i]] = 1;
  }

  for(bm in bmlist){
    oo = bmstat(bm, ttimear);
    print bm" Frontend "oo;
    oo = bmstat(bm, ar);
    print bm" Solver "oo;
    oo = bmstat(bm, synth);
    print bm" Synthesis "oo;
    oo = bmstat(bm, beoverhead);
    print bm" Verification "oo;
    oo = bmstat(bm, verif);
    print bm" BEOverhead "oo;
    oo = bmstat(bm, nodes);
    print bm" Nodes "oo;
    oo = bmstat(bm, synthClause);
    print bm" Sclauses "oo;

  }

}

function hlreport(){
  print "Frontend Time: "
  printstats(ttimear);
  print "Total Solver Time:"
  printstats(ar);
  print "Total Synthesis Time:"
  printstats(synth);
  print "Synthesis Clauses for Last Iteration:"
  printstats(synthClause);
  print "Backend Overhead: "
  printstats(beoverhead);
}


END{
  detailed();
}

