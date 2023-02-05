OBJS	= ../objects/frontend/main.o ../objects/frontend/io.o ../objects/frontend/frontend.o ../objects/frontend/insparse.o
SOURCE	= main.c io/io.c frontend/frontend.c insparse/insparse.c
HEADER	= io/io.h frontend/frontend.h insparse/insparse.h
OUT	= ../binaries/frontend
CC	 = gcc
FLAGS	 = -g -Wall
LFLAGS	 = 

all: createFolders $(OBJS)
	$(CC) $(FLAGS) $(OBJS) -o $(OUT) $(LFLAGS)

../objects/frontend/main.o: main.c
	$(CC) -c $(FLAGS) main.c -o ../objects/frontend/main.o

../objects/frontend/io.o: io/io.c
	$(CC) -c $(FLAGS) io/io.c -o ../objects/frontend/io.o

../objects/frontend/frontend.o: frontend/frontend.c
	$(CC) -c $(FLAGS) frontend/frontend.c -o ../objects/frontend/frontend.o

../objects/frontend/insparse.o: insparse/insparse.c
	$(CC) -c $(FLAGS) insparse/insparse.c -o ../objects/frontend/insparse.o

createFolders:
ifeq ($(wildcard ../binaries/.*),)
	mkdir ../binaries
endif
ifeq ($(wildcard ../objects/.*),)
	mkdir ../objects
endif
ifeq ($(wildcard ../objects/frontend/.*),)
	mkdir ../objects/frontend/
endif

clean:
ifneq ($(wildcard ../objects/frontend/.*),)
	rm -r ../objects/frontend/
endif
	rm -f $(OUT)
