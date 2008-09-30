(benchmark test1 
	:notes "
if (x<y+c) y = x;
else x = y
assert x==y 
	"
	:extrafuns ((c Int)(x Int)(y Int)(y1 Int)(y2 Int)(x2 Int)(x1 Int)(t bool))
	:formula
			(and 	
					(= x 0)
					(= y 2)
					(not (= c -2))
					(not (= c -3))
					(not (= c -1))
					(= t (< x (+ y c)))
				    (= x1 y)
				    (= y1 x)
				    (ite t (= x2 x1) (= x2 x))
				    (ite t (= y2 y) (= y2 y1))
				    (= x2 y2)	
			 ) 
)


