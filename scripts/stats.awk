BEGIN{
        ptime = 0.0;
        betime = 0.0;
	stime = 0.0;
	lastSclauses = 0;
        i=1;
}
/f#.*clauses/{
	lastSclauses = $5;
}
/Total time =/{
       
       ar[i] = ptime;
       beoverhead[i] = betime-ptime;
       ttimear[i] = ($4/1000.0)-betime;
       synth[i] = stime;
       synthClause[i] = lastSclauses;
       ptime = 0.0;
       betime = 0.0;
       stime = 0.0;
	lastSclauses=0;
       i=i+1;
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

END{
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
