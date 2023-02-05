#ifndef ENTITY_H
#define ENTITY_H

#include <vector>

#include "../shared.h"

class Entity
{
protected:
	int id = -1;
	bool alive = true;
	
	// visual information
	//
	int range{ };
	char visual_representation{ };
	COORD_PAIR coords{ };

public:
	Entity( );
	
	~Entity( );
	
	void remove( );
	
	// manage location related entity variables
	//
	COORD_PAIR getCoords( );
	
	[[nodiscard]] bool isAlive( ) const;
	
	[[nodiscard]] int getId( ) const;
	
	[[nodiscard]] char getVisualChar( ) const;
	
	// enforcing LSP design principle (one of SOLID object-oriented design principles)
	//
	virtual void onDeath( ) { }
	
	virtual void processTick( ) { }
	
	virtual bool moveTo( COORD_PAIR new_coords );
	
	virtual std::shared_ptr< Entity > getClone( )
	{
		return std::make_shared< Entity >( *this );
	}
};

typedef std::shared_ptr< Entity > EntityPtr;

#endif //ENTITY_H
