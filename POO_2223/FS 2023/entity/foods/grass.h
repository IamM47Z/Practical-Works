#ifndef GRASS_H
#define GRASS_H

#include "food.h"

class Grass : public Food
{
	bool reproduced = false;

public:
	Grass( );
	
	void processTick( ) override;
	
	std::shared_ptr< Entity > getClone( ) override;
};

#endif //GRASS_H