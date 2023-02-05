import React from 'react';
import './app.css';

import { numGen, letterGen, getIndexByDirection, getInfoByPoints, timeStrToSeconds } from './helpers/utils';

import Counter  from './components/counter/counter.component.jsx';
import GameBoard  from './components/gameboard/gameboard.component.jsx';
import WordsPanel  from './components/wordspanel/wordspanel.component.jsx';
import ScoreBoard  from './components/scoreboard/scoreboard.component.jsx';
import ControlPanel  from './components/controlpanel/controlpanel.component.jsx';

let dict_array = [
    {
        "word": "victory"
    },
    {
        "word": "butterfly"
    },
    {
        "word": "Yoda"
    },
    {
        "word": "luck"
    },
    {
        "word": "unicorn"
    },
    {
        "word": "battleship"
    },
    {
        "word": "saltwater"
    },
    {
        "word": "clean"
    },
    {
        "word": "antelope"
    },
    {
        "word": "palm tree"
    },
    {
        "word": "mohawk"
    },
    {
        "word": "supermarket"
    },
    {
        "word": "Pikachu"
    },
    {
        "word": "Wall-e"
    },
    {
        "word": "flag"
    },
    {
        "word": "equator"
    },
    {
        "word": "plague"
    },
    {
        "word": "viola"
    },
    {
        "word": "stop sign"
    },
    {
        "word": "drain"
    },
    {
        "word": "planet"
    },
    {
        "word": "cute"
    },
    {
        "word": "neck"
    },
    {
        "word": "sad"
    },
    {
        "word": "old"
    },
    {
        "word": "cake"
    },
    {
        "word": "gas"
    },
    {
        "word": "collapse"
    },
    {
        "word": "virus"
    },
    {
        "word": "turkey"
    },
    {
        "word": "hairy"
    }
];

class App extends React.Component
{
    constructor()
    {
        super();

        this.state = {
            // game info
            level: 0,
            max_time: 3 * 60,
        
            words: [],
            words_array: [],
            num_unspoted_words: 0,

            letters_array: [],
            letters_per_row: 8,

            game_state: -1,          // 0 - lose | 1 - win
            game_started: false,

            // highlight and check mechanisms
            reset_board: false,
            letter_begin_id: -1,
            register_counter: 0,
            highlighted_elems: [],

            // events
            event_mouseup: null,
            event_mousemove: null,
            event_mousedown: null
        };

        this.addWord = this.addWord.bind( this );
        this.onMouseUp = this.onMouseUp.bind( this );
        this.removeWord = this.removeWord.bind( this );
        this.onMouseMove = this.onMouseMove.bind( this );
        this.onMouseDown = this.onMouseDown.bind( this );
        this.changeLevel = this.changeLevel.bind( this );
        this.toggleGameState = this.toggleGameState.bind( this );
    }

    componentDidMount( )
    {
        let req = new XMLHttpRequest( );
    
        req.open( "GET", "https://raw.githubusercontent.com/Logfro/SkribblBot/master/words_english.json", false );
        req.send( null );
    
        if ( req.status !== 200 )
            return;
    
        // update our dictionary with an online json
        dict_array = JSON.parse( req.responseText );
        let words = JSON.parse( localStorage.getItem( "words" ) );
        if ( !words )
            words = [];

        let event_mouseup = document.addEventListener( 'mouseup', this.onMouseUp );
        let event_mousemove = document.addEventListener( 'mousemove', this.onMouseMove );
        let event_mousedown = document.addEventListener( 'mousedown', this.onMouseDown );

        this.changeLevel( this.state.level );

        this.setState( { event_mouseup: event_mouseup, event_mousemove: event_mousemove, event_mousedown: event_mousedown, words: words } );
    }

    componentWillUnmount( )
    {
        if ( this.state.event_mouseup )
            document.removeEventListener( 'mouseup', this.state.event_mouseup );
        if ( this.state.event_mousemove )
            document.removeEventListener( 'mousemove', this.state.event_mousemove );
        if ( this.state.event_mousedown )
            document.removeEventListener( 'mousedown', this.state.event_mousedown );

        this.setState( { event_mouseup: null, event_mousemove: null, event_mousedown: null } );
    }

    componentDidUpdate( _, prev )
    {
        if ( prev.reset_board !== this.state.reset_board && this.state.reset_board )
        {
            console.log( "reseting reset value\n" );
            this.setState( { reset_board: false } );
        }

        if ( prev.num_unspoted_words !== this.state.num_unspoted_words && !this.state.num_unspoted_words )
        {
            this.toggleGameState( );
        }
    }

    onMouseUp( e )
    {
        if ( this.state.letter_begin_id === -1 || !this.state.game_started )
            return;
        
        e.preventDefault( );

        for ( let i = 0; i < this.state.highlighted_elems.length; i++ )
        {
            let elem = this.state.highlighted_elems[i];
            elem.classList.remove( "active" );
        }

        let elem = document.elementFromPoint( e.clientX, e.clientY );
        if ( elem.classList.contains( "letter-square" ) )
        {
            let valid = false;
            for ( let i = 0; i < this.state.words_array.length; i++ )
            {
                const word = this.state.words_array[ i ];
                if ( word.begin_index !== this.state.letter_begin_id || word.end_index !== parseInt( elem.id ) )
                    continue;

                valid = true;
                let temp_words_array = this.state.words_array.slice();
                temp_words_array[i].class += " spoted";
                this.setState( { words_array: temp_words_array } );
                break;
            }

            if ( valid )
            {
                for ( let i = 0; i < this.state.highlighted_elems.length; i++ )
                    this.state.highlighted_elems[i].classList.add( "valid" );
                
                elem.classList.add( "valid" );

                return this.setState( { letter_begin_id: -1, highlighted_elems: [], num_unspoted_words: this.state.num_unspoted_words - 1 } );
            }
        }

        this.setState( { letter_begin_id: -1, highlighted_elems: [] } );
    }

    onMouseMove( e )
    {
        if ( this.state.letter_begin_id === -1 || !this.state.game_started )
            return;
        
        e.preventDefault( );

        let elem = document.elementFromPoint( e.clientX, e.clientY );
        if ( elem.classList.contains( "letter-square" ) )
        {
            for ( let i = 1; i < this.state.highlighted_elems.length; i++ )
            {
                let elem = this.state.highlighted_elems[i];
                elem.classList.remove( "active" );
            }

            let [ direction, size ] = getInfoByPoints( this.state.letters_per_row, this.state.letters_per_row, this.state.letter_begin_id, elem.id );
            if ( direction === -1 )
                return;

            let highlighted_elems = [];
            for ( let i = 0; i < size + 1; i++ )
            {
                let index = getIndexByDirection( direction, this.state.letters_per_row, this.state.letters_per_row, this.state.letter_begin_id, i );
                let elem = document.querySelector( '.letter-square[id="' + index + '"]' );
                elem.classList.add( "active" );
                highlighted_elems.push( elem );
            }

            this.setState( { highlighted_elems: highlighted_elems } );
        }
    }

    onMouseDown( e )
    {
        if ( !this.state.game_started )
            return;

        let elem = document.elementFromPoint( e.clientX, e.clientY );
        if ( !elem.classList.contains( "letter-square" ) )
            return;
        
        e.preventDefault( );

        this.setState( { letter_begin_id: parseInt( elem.id ) } );
    }

    getLevelStr( str_type = 0 )
    {
        switch( this.state.level )
        {
            case 0:
                return ( !str_type ? "easy" : ( str_type === 1 ? "Easy" : "EASY" ) );
            case 1:
                return ( !str_type ? "medium" : ( str_type === 1 ? "Medium" : "MEDIUM" ) );
            case 2:
                return ( !str_type ? "hard" : ( str_type === 1 ? "Hard" : "HARD" ) );
            default:
                return null;
        }
    }
 
    changeLevel( new_level )
    {
        if ( this.state.game_started )
            return;

        let letters_per_row = 8;
        switch( new_level )
        {
            case 0:
                break;
            case 1:
                letters_per_row = 10;
                break;
            case 2:
                letters_per_row = 12;
                break;
            default:
                return;
        }
        
        let letters_array = [];
        for ( let i = 0; i < letters_per_row * letters_per_row; i++ )
        {
            if ( !letters_array[ i ] )
                letters_array[ i ] = letterGen( );
        }

        this.setState( { level: new_level, letters_per_row: letters_per_row, letters_array: letters_array, reset_board: true } );
    }

    toggleGameState( )
    {
        if ( this.state.game_started )
            this.onGameStop( );
        else
            this.onGameStart( );
    }

    onGameStart( )
    {
        let num_words = 3;
        switch ( this.state.level )
        {
            case 0:
                break;
            case 1:
                num_words = 5;
                break;
            case 2:
                num_words = 8;
                break;
            default:
                return;
        }        
    
        let words_array = [];
        if (num_words >= this.state.words.length)
        {
            for ( let i = 0; i < this.state.words.length; i++, num_words-- )
                words_array.push( { word: this.state.words[ i ], class: "word" } );
        }
        else
        {
            let indexes = [];
            for ( let i = 0; i < num_words; i++ )
            {
                let index = numGen( 0, this.state.words.length - 1 );
                if ( indexes.includes( index ) )
                {
                    i--;
                    continue;
                }

                indexes.push( index );
                words_array.push( { word: this.state.words[ index ].word.toLowerCase(), class: "word" } );
            }
        }

        console.log( words_array );
        
        let indexes = [];
        for ( let i = 0; i < num_words; i++ )
        {
            let index = numGen( 0, dict_array.length - 1 );
            if ( indexes.includes( index ) )
            {
                i--;
                continue;
            }

            indexes.push( index );
            words_array.push( { word: dict_array[ index ].word.toLowerCase(), class: "word" } );
        }
        
        for ( let i = 0; i < num_words; i++ )
        {
            let rand_index = numGen( 0, dict_array.length - 1 );
            if ( dict_array[ rand_index ].word.length > this.state.letters_per_row || 
                dict_array[ rand_index ].word.indexOf( " " ) > -1 || !/^[a-z]+$/i.test( dict_array[ rand_index ].word ) ||
                dict_array[ rand_index ].word.length < 2 )
            {
                i--;
                continue;
            }

            // we could use .some() but since our dict_array would stay outside of the scope we would get a warning
            let new_word = true;
            for ( let j = 0; j < words_array.length; j++ )
            {
                if ( words_array[ j ].word !== dict_array[ rand_index ].word )
                    continue;
                
                new_word = false;
                break;
            }

            if ( !new_word )
            {
                i--;
                continue;
            }

            words_array.push( { word: dict_array[ rand_index ].word.toLowerCase(), class: "word" } );   
        }
    
        let letters_array = [];
    
        for ( let i = 0; i < words_array.length; i++ )
        {
            const word = words_array[ i ].word;

            let rand_index = numGen( 0, this.state.letters_per_row * this.state.letters_per_row - 1 );
            if ( letters_array[ rand_index ] !== word[ 0 ] && letters_array[ rand_index ] )
            {
                i--;
                continue;
            }

            let valid_directions = [];
            for ( let direction = 0; direction < 8; direction++ )
            {
                let valid_direction = true;
                for ( let j = 1; j < word.length; j++ )
                {
                    let check_index = getIndexByDirection( direction, this.state.letters_per_row, this.state.letters_per_row, rand_index, j );
                    if ( check_index < 0 || ( letters_array[ check_index ] !== word[ j ] && letters_array[ check_index ] ) )
                        valid_direction = false;
                
                    if ( !valid_direction )
                        break;
                }

                if ( valid_direction )
                    valid_directions.push( direction );
            }

            if ( !valid_directions.length )
            {
                i--;
                continue;
            }

            let rand_direction = valid_directions[ numGen( 0, valid_directions.length - 1 ) ];
            for ( let j = 0; j < word.length; j++ )
                letters_array[ getIndexByDirection(rand_direction, this.state.letters_per_row, this.state.letters_per_row, rand_index, j) ] = word[ j ];
        
            words_array[ i ].begin_index = rand_index;
            words_array[ i ].end_index = getIndexByDirection(rand_direction, this.state.letters_per_row, this.state.letters_per_row, rand_index, word.length - 1);
        }
    
        for ( let i = 0; i < this.state.letters_per_row * this.state.letters_per_row; i++ )
        {
            if ( !letters_array[ i ] )
                letters_array[ i ] = letterGen( );
        }

        this.setState( { register_counter: 0, letters_array: letters_array, words_array: words_array, num_unspoted_words: words_array.length, game_started: true, game_state: -1, reset_board: true } );
    }
    
    onGameStop( )
    {
        if (this.state.num_unspoted_words !== 0)
        {
            this.setState( {  register_counter: 0, game_state: -1, game_started: false } );
            return;
        }

        let counter = timeStrToSeconds( document.getElementsByClassName( 'counter-time' )[0].innerHTML );

        let game_state = 1;
        if ( counter > this.state.max_time )
            game_state = 0;

        if ( game_state )
        {
            // show victory
        }
        else
        {
            // show lose
        }

        this.setState( { register_counter: counter, game_state: game_state, game_started: false } );
    }

    addWord( )
    {
        let word = document.getElementById( "new_word" ).value.toLowerCase( );
        if ( word.length > this.state.letters_per_row || word.indexOf( " " ) > -1 || word.length < 2 
         || !/^[a-z]+$/i.test( word ) )
            return;

        if (!this.state.words.includes( word ))
        {
            this.setState( { words: [ ...this.state.words, word ] } );
            localStorage.setItem( "words", JSON.stringify( [ ...this.state.words, word ] ) );
            document.getElementById( "new_word" ).value = "";
        }
    }

    removeWord( )
    {
        let word = document.getElementById( "new_word" ).value.toLowerCase( );
    
        if (this.state.words.includes( word ))
        {
            let words = this.state.words;
            words.splice( words.indexOf( word ), 1 );
            this.setState( { words: words } );
            localStorage.setItem( "words", JSON.stringify( words ) );
            document.getElementById( "new_word" ).value = "";
        }
    }

    render( )
    {
        return (
            <div className="App">
                <br/>
                <h1 id="Title" >Sopa de Letras</h1>
                <br/>
                <div className='mainboard-container'>
                    <div className='mainboard'>
                        <div className='left'>
                            <ControlPanel game_started={ this.state.game_started } level_str={ this.getLevelStr( 1 ) } toggleGameState={ this.toggleGameState } changeLevel={ this.changeLevel } addWord={ this.addWord } removeWord={ this.removeWord } words={ this.state.words }/>
                            <ScoreBoard counter={ this.state.register_counter } game_state={ this.state.game_state } difficulty={ this.getLevelStr( 1) } />
                        </div>
                        <div className='center'>
                            <GameBoard letters_array={ this.state.letters_array } level_str={ this.getLevelStr( ) } reset_board={ this.state.reset_board }/>
                        </div>
                        <div className='right'>
                            <WordsPanel words_array={ this.state.words_array } num_unspoted_words={ this.state.num_unspoted_words }/>
                        </div>
                    </div>
                </div>
                <Counter max_value={ this.state.max_time } game_started={ this.state.game_started } toggleGameState={ this.toggleGameState }/>
                
            </div>
        );
    }
}

export default App;
