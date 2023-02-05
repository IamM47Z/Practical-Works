#ifndef CARROT_H
#define CARROT_H

#include "food.h"

class Carrot : public Food
{
public:
	Carrot( );
	
	void processTick( ) override;
	
	std::shared_ptr< Entity > getClone( ) override;
};


#endif //CARROT_H