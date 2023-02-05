#include "carrot.h"

#include "../../consts/consts.h"

Carrot::Carrot( ) : Food( "Carrot" )
{
	visual_representation = 't';
	
	duration = -1;
	toxicity_points = 0;
	nutritive_points = 4;
	
	if ( !addSmell( SMELL::VERDURA ) )
	{
		endwin( );
		perror( "Fatal error initializing Carrot Smells" );
		exit( 1 );
	}
}

void Carrot::processTick( )
{
	age++;
	
	if ( age <= 30 && !( age % 10 ) )
		toxicity_points++;
}

std::shared_ptr< Entity > Carrot::getClone( )
{
	auto pclone = std::make_shared< Carrot >( );
	
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