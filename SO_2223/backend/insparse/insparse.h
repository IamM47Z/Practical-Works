#ifndef INSPARSE_H
#define INSPARSE_H

typedef enum
{
	USERS,      // users
	LIST,       // list
	KICK,       // kick <username>
	PROM,       // prom
	REPROM,     // reprom
	CANCEL,     // cancel <promoter-name>
	CLOSE,      // close
	HELP        // help
} INS_TYPE;

typedef struct
{
	INS_TYPE type;
	char buffer[25];        // only used for kick and cancel commands
} INS_INFO, * PINS_INFO;

extern const char* help_messages[HELP + 1];

void showInstructionSyntax( INS_TYPE type );

bool parseInstruction( char* ins_buffer, size_t buffer_size, PINS_INFO pout );

#endif //INSPARSE_H
