.PHONY: build clean 

build: ./bin/clox

./bin/clox:
	@mkdir -p ./bin
	clang -Wall src/*.c -o ./bin/clox

clean:
	@rm -f ./bin/clox

