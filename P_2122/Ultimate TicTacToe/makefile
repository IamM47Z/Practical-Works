OBJS	= ../objects/main.o ../objects/dialog.o ../objects/gameengine.o ../objects/insparse.o ../objects/misc.o ../objects/saveengine.o ../objects/vector.o
SOURCE	= main.c Dialog/dialog.c GameEngine/gameengine.c InsParse/insparse.c Misc/misc.c SaveEngine/saveengine.c Vector/vector.c
HEADER	= Misc/misc.h Vector/vector.h SaveEngine/saveengine.h InsParse/insparse.h GameEngine/gameengine.h Dialog/dialog.h
OUT     = ../binaries/main

CC      = gcc
FLAGS   = -g -c -Wall

all: createFolders $(OBJS)
	$(CC) -g $(OBJS) -o $(OUT)

../objects/main.o: main.c
	$(CC) $(FLAGS) main.c -std=c99 -o ../objects/main.o

../objects/dialog.o: Dialog/dialog.c
	$(CC) $(FLAGS) Dialog/dialog.c -std=c99 -o ../objects/dialog.o

../objects/gameengine.o: GameEngine/gameengine.c
	$(CC) $(FLAGS) GameEngine/gameengine.c -std=c99 -o ../objects/gameengine.o

../objects/insparse.o: InsParse/insparse.c
	$(CC) $(FLAGS) InsParse/insparse.c -std=c99 -o ../objects/insparse.o

../objects/misc.o: Misc/misc.c
	$(CC) $(FLAGS) Misc/misc.c -std=c99 -o ../objects/misc.o

../objects/saveengine.o: SaveEngine/saveengine.c
	$(CC) $(FLAGS) SaveEngine/saveengine.c -std=c99 -o ../objects/saveengine.o

../objects/vector.o: Vector/vector.c
	$(CC) $(FLAGS) Vector/vector.c -std=c99 -o ../objects/vector.o

createFolders:
ifeq ($(wildcard ../binaries/.*),)
	mkdir ../binaries
endif
ifeq ($(wildcard ../objects/.*),)
	mkdir ../objects
endif

clean:
	rm -f $(OUT)
ifneq ($(wildcard ../objects/.*),)
	rm -r ../objects/
endif

rebuild: clean all