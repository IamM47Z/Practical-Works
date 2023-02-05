#include "beef.h"

#include "../../consts/consts.h"

Beef::Beef( ) : Food( "Beef" )
{
	visual_representation = 'b';
	
	duration = 30;
	toxicity_points = 2;
	nutritive_points = 10;
	
	if ( !addSmell( SMELL::CARNE ) || !addSmell( SMELL::KETCHUP ) )
	{
		endwin( );
		perror( "Fatal error initializing Beef Smells" );
		exit( 1 );
	}
}

void Beef::processTick( )
{
	age++;
	
	if ( nutritive_points > 0 )
		nutritive_points--;
}

std::shared_ptr< Entity > Beef::getClone( )
{
	auto pclone = std::make_shared< Beef >( );
	
	// entity class
	//
	pclone->id = id;
	pclone->alive = alive;
	
	pclone->range = range;
	pclone->visual_representation = visual_representation;
	pclone->coords = coords;
	
	// foodentry class
	//
	pclone->species = species;
	pclone->nutritive_points = nutritive_points;
	pclone->toxicity_points = toxicity_points;
	
	// food class
	//
	pclone->age = age;
	pclone->duration = duration;
	pclone->smells = smells;
	
	return pclone;
}