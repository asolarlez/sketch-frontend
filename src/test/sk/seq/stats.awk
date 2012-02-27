BEGIN{
        ptime = 0.0;
        betime = 0.0;
        i=1;
}

/Total time =/{
       
       ar[i] = ptime;
       beoverhead[i] = betime-ptime;
       ttimear[i] = ($4/1000.0)-betime;
       ptime = 0.0;
       betime = 0.0;
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

function printstats(a){

	n=asort(a); 
	k=n/9; 
	i=1; 
	it=0; 
	while(i<=n){ 
		it=it+1; 
                cnt[it] = 0;
                tot[it] = 0.0;
                mn[it] = 0.0;
                mx[it] = 0.0;
		for(t=1; t<=k && i<=n; t++){ 
                        mx[it] = a[i];
                        if(t==1){ mn[it] = a[i]; }
			cnt[it] = cnt[it] + 1;
			tot[it] = tot[it] + a[i];
			i=i+1;
		}
	}
        for(k=1; k<=it; ++k){
		print" decil "k"  ["mn[k]", "mx[k]"] cnt="cnt[k]"  avg="(tot[k]/cnt[k]);

	}	 
}

END{
  print "Total time stats: "
  printstats(ttimear);
  print "Solver time stats:" 
  printstats(ar);
  print "Backend overhead stats: "
  printstats(beoverhead);
} 
