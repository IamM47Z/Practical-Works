OBJS	= ../objects/backend/main.o ../objects/backend/auction.o ../objects/backend/promotion.o ../objects/backend/io.o ../objects/backend/backend.o ../objects/backend/promoter.o ../objects/backend/insparse.o ../objects/backend/client.o
SOURCE	= main.c auction/auction.c promotion/promotion.c io/io.c backend/backend.c promoter/promoter.c insparse/insparse.c client/client.c
HEADER	= auction/auction.h promotion/promotion.h users/users_lib.h io/io.h backend/backend.h promoter/promoter.h insparse/insparse.h client/client.h
OUT	= ../binaries/backend
CC	 = gcc
FLAGS	 = -g -Wall
LFLAGS 	 = -lm -lpthread
ROBJS	 = users/users_lib.o

all: createFolders $(OBJS)
	$(CC) $(FLAGS) $(OBJS) $(ROBJS) -o $(OUT) $(LFLAGS)

../objects/backend/main.o: main.c
	$(CC) -c $(FLAGS) main.c -o ../objects/backend/main.o

../objects/backend/auction.o: auction/auction.c
	$(CC) -c $(FLAGS) auction/auction.c -o ../objects/backend/auction.o

../objects/backend/promotion.o: promotion/promotion.c
	$(CC) -c $(FLAGS) promotion/promotion.c -o ../objects/backend/promotion.o

../objects/backend/io.o: io/io.c
	$(CC) -c $(FLAGS) io/io.c -o ../objects/backend/io.o

../objects/backend/backend.o: backend/backend.c
	$(CC) -c $(FLAGS) backend/backend.c -o ../objects/backend/backend.o

../objects/backend/promoter.o: promoter/promoter.c
	$(CC) -c $(FLAGS) promoter/promoter.c -o ../objects/backend/promoter.o

../objects/backend/insparse.o: insparse/insparse.c
	$(CC) -c $(FLAGS) insparse/insparse.c -o ../objects/backend/insparse.o

../objects/backend/client.o: client/client.c
	$(CC) -c $(FLAGS) client/client.c -o ../objects/backend/client.o

createFolders:
ifeq ($(wildcard ../binaries/.*),)
	mkdir ../binaries
endif
ifeq ($(wildcard ../objects/.*),)
	mkdir ../objects
endif
ifeq ($(wildcard ../objects/backend/.*),)
	mkdir ../objects/backend/
endif

clean:
ifneq ($(wildcard ../objects/backend/.*),)
	rm -r ../objects/backend/
endif
	rm -f $(OUT)
