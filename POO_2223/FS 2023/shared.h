#ifndef SHARED_H
#define SHARED_H

#include <utility>

#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
#include "curses.h"
#else

#include <ncurses.h>

#endif

#define COORD_PAIR std::pair< int, int >

#define TERM_RESERVE_X          0.015f
#define TERM_RESERVE_Y          0.05f
#define TERM_SQUARE_WIDTH       0.125f
#define TERM_SQUARE_HEIGHT      0.25f
#define TERM_RESERVE_WIDTH      0.9f
#define TERM_RESERVE_HEIGHT     0.45f

struct DIRECTION
{
	union
	{
		struct
		{
			bool up: 1;
			bool down: 1;
			bool left: 1;
			bool right: 1;
		};
		int value: 4;
	};
};

enum SMELL
{
	ERVA,
	VERDURA,
	CARNE,
	KETCHUP
};

enum SPECIE
{
	KANGAROO,
	RABBIT,
	SHEEP,
	WOLF
};

#endif //SHARED_H
