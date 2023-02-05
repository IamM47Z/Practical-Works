import React from 'react';
import './scoreboard.css';

import { secondsToTimeStr } from '../../helpers/utils';

class ScoreBoard extends React.Component
{
    constructor( props )
    {
        super( props );

        this.state = {
            database: [ ]
        };
    }

    componentDidMount( )
    {
        let scoreboard = JSON.parse( localStorage.getItem( 'scoreboard' ) );
        if ( !scoreboard )
            return;

        if ( scoreboard.length )
            scoreboard.sort( ( a, b ) => a.counter - b.counter );
        
        this.setState( { database: scoreboard } );
    }

    componentWillUnmount( )
    {
        localStorage.setItem( 'scoreboard', JSON.stringify( this.state.database ) );
    }

    componentDidUpdate( prev )
    {
        if ( prev.game_state === this.props.game_state || this.props.game_state === -1 )
            return;

        let temp_database = this.state.database.slice( );
        temp_database.push( { counter: this.props.counter, name: prompt( "Insira o nome:" ), difficulty: this.props.difficulty } );
        temp_database.sort( ( a, b ) => a.counter - b.counter );
        temp_database = temp_database.slice( 0, 10 );
        
        localStorage.setItem( 'scoreboard', JSON.stringify( temp_database ) );
        this.setState( { database: temp_database } );
    }

    renderScoreboard( )
    {
        if ( !this.state.database.length )
            return ( <div>Empty! Give it a try :D</div> );

        return this.state.database.map(
            ( counter ) => {
                return ( <div className='scoreboard-value'> { 
                    secondsToTimeStr( counter.counter ) + " - " + counter.name + " - " + counter.difficulty } </div> );
            }
        );
    }

    render( ) 
    {
        return ( 
            <div className='scoreboard'>
                <h4>ScoreBoard:</h4>
                { this.renderScoreboard( ) }
            </div>
        );
    }
}

export default ScoreBoard;