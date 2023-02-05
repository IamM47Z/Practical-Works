#ifndef INSPARSE_H
#define INSPARSE_H

typedef enum
{
	SELL,       // sell <name> <category> <price> <buy-now-price> <duration>
	LIST,       // list
	LICAT,      // licat <category>
	LISEL,      // lisel <username>
	LIVAL,      // lival <max-price>
	LITIME,     // litime <max-time>
	TIME,       // time
	BUY,        // buy <id> <value>
	CASH,       // cash
	ADD,        // add <value>
	EXIT,       // exit
	HELP
} INS_TYPE;

typedef struct
{
	INS_TYPE type;
	union
	{
		struct
		{
			char name[25];
			char category[25];
			unsigned int price;
			unsigned int buy_now_price;
			unsigned int duration;
		};
		char buffer[25];      // this works for category and username
		int int_value;        // this works for all single argument integer commands
		struct
		{
			unsigned int id;
			unsigned int bid_value;
		};
	};
} INS_INFO, * PINS_INFO;

extern const char* help_messages[HELP + 1];

void showInstructionSyntax( INS_TYPE type );

bool parseInstruction( char* ins_buffer, size_t buffer_size, PINS_INFO pout );

#endif //INSPARSE_H
