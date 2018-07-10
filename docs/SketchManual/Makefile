manual.pdf: *.tex ../../src/sketchlib/*.skh
	sketch --fe-output-docs math.tex ../../src/sketchlib/math.skh
	sketch --fe-output-docs list.tex ../../src/sketchlib/list.skh
	sketch --fe-output-docs generators.tex ../../src/sketchlib/generators.skh
	sketch --fe-output-docs array.tex ../../src/sketchlib/array.skh
	sketch --fe-output-docs stack.tex ../../src/sketchlib/stack.skh
	sketch --fe-output-docs generics.tex ../../src/sketchlib/generics.skh
	pdflatex manual
	makeglossaries manual
	pdflatex manual
