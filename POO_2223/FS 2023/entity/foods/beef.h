#ifndef BEEF_H
#define BEEF_H

#include "food.h"

class Beef : public Food
{
public:
	Beef( );
	
	void processTick( ) override;
	
	std::shared_ptr< Entity > getClone( ) override;
};


#endif //BEEF_H