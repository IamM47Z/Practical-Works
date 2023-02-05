import React from 'react';
import './counter.css';

import { secondsToTimeStr } from '../../helpers/utils'

class Counter extends React.Component
{
    constructor( props )
    {
        super( props );

        this.state = {
            counter: 0,
            interval: null,
            working: false
        };
        
        this.increase = this.increase.bind( this );
    }

    componentWillUnmount( )
    {
        if ( !this.state.working )
            return;
        
        clearInterval( this.state.interval );
    }

    componentDidUpdate( prev )
    {
        if ( prev.game_started !== this.props.game_started )
            return;

        if ( this.props.game_started )
            this.start( );
        else
            this.stop( );
    }

    increase( )
    {
        if ( this.state.counter === this.props.max_value )
            return this.props.toggleGameState( );

        let new_counter = this.state.counter + 1;
        this.setState( { counter: new_counter } );
    }

    start( )
    {
        if ( this.state.working )
            return;
        
        this.setState( { counter: 0, interval: setInterval( this.increase, 1000 ), working: true } );
    }

    stop( )
    {
        if ( !this.state.working )
            return;
            
        clearInterval( this.state.interval );
        this.setState( { interval: null, working: false } );
    }

    render( ) 
    {
        return ( 
            <div className='counter'>
                <h5>Counter:&nbsp;</h5> 
                <h5 className='counter-time'>{ secondsToTimeStr( this.state.counter ) }</h5>
            </div>
        );
    }
}

export default Counter;