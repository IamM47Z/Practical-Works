let toggled_elem = null

function toggle_class(elem, class_name)
{
    if ( elem.classList.contains(class_name) )
    {
        toggled_elem = null;
        elem.classList.remove(class_name);
        return;
    }

    elem.classList.add(class_name);
    if ( toggled_elem )
        toggled_elem.classList.remove(class_name);

    toggled_elem = elem;
}

function set_dropdown_listeners()
{
    let dropdown_btns_list = document.getElementsByClassName('dropdown-btn');
    let picanha_btns_list = document.getElementsByClassName('picanha-btn');
    if (!dropdown_btns_list.length || !picanha_btns_list.length)
        return setTimeout(set_dropdown_listeners, 200);

    Array.prototype.forEach.call(dropdown_btns_list, dropdown_btn => {
        dropdown_btn.addEventListener('click', () => {
            toggle_class(dropdown_btn.nextElementSibling, 'open');
        }, true);
    });

    Array.prototype.forEach.call(picanha_btns_list, picanha_btn => {
        picanha_btn.addEventListener('click', () => {
            toggle_class(picanha_btn.nextElementSibling, 'open');
        }, true);
    });
}

function calc_deval_coefficient( age, garage, near_public_transports )
{
    let c1 = 0.9, c2 = 0.95, c3 = 0.9;

    if ( age <= 5 )
        c1 = 1;
    else if ( age <= 10 )
        c1 = 0.95;

    if ( garage )
        c2 = 1;
    
    if ( near_public_transports )
        c3 = 1;

    return c1 * c2 * c3;
}

function generate_num(min, max) 
{
    return Math.floor(Math.random() * (Math.floor(max) - Math.ceil(min))) + Math.ceil(min);
}

function calc_imobile_price( )
{
    let form = document.getElementById( "search_form" );
    
    if ( !form.checkValidity() )
    {
        alert("Formulário Inválido!");
        return;
    }

    let type = form.querySelector("#type-select").value;
    let area = form.querySelector("input[name=\"area\"]").value; 
    let age = form.querySelector("input[name=\"age\"]").value; 
    let garage = form.querySelector("#garage-checkbox").checked; 
    let near_public_transports = form.querySelector("#public-transports-checkbox").checked;

    let deval_coefficient = calc_deval_coefficient( age, garage, near_public_transports );

    const zone_a = 1200 * deval_coefficient, zone_b = 2000 * deval_coefficient, zone_c = 2500 * deval_coefficient;

    let result_area = document.getElementById( "search_result" );

    result_area.querySelector( "#zone_a_price" ).innerText = Math.round( zone_a * area );
    result_area.querySelector( "#zone_b_price" ).innerText = Math.round( zone_b * area );
    result_area.querySelector( "#zone_c_price" ).innerText = Math.round( zone_c * area );

    result_area.querySelectorAll( "#type" ).forEach( ( value ) => {
        if ( type == "over_T10" )
            value.innerText = "T" + generate_num( 10, 16 );
        else if ( type == "all" )
            value.innerText = "T" + generate_num( 1, 16 );
        else 
            value.innerText = type;
    });

    result_area.querySelectorAll( "#area" ).forEach( ( value ) => {
        value.innerText = area;
    });

    result_area.style.display = "flex";
}

function calc_financing( )
{
    let form = document.getElementById( "financing_form" );
    
    if ( !form.checkValidity() )
    {
        alert("Formulário Inválido!");
        return;
    }

    let value = form.querySelector("input[name=\"value\"]").value; 
    let initial_value = form.querySelector("input[name=\"initial_value\"]").value;
    let finance_value = value - initial_value;
    let deadline = form.querySelector("input[name=\"deadline\"]").value;

    Array.prototype.forEach.call( document.getElementsByClassName("financing_box"), ( finance_box ) => {
        let spread = generate_num(100, 500) / 100;
        let tax = Math.round( ( 0.5 + spread ) * 100 ) / 100;

        let month_price = finance_value / ( deadline * 12 );
        month_price += month_price * ( tax / 100 );

        finance_box.querySelector( "#financing_price" ).innerText = finance_value;
        finance_box.querySelector( "#financing_spread" ).innerText = spread;
        finance_box.querySelector( "#financing_tax" ).innerText = tax;
        finance_box.querySelector( "#financing_month_price" ).innerText = Math.round( month_price );
    } );

    document.getElementById( "financing_result" ).style.display = "flex";
    document.getElementById( "financing_query" ).style.display = "none";
}

function toogle_financing_screen()
{
    let display = "block";
    if (document.getElementById( "financing_screen" ).style.display == "block")
    {
        display = "none";

        document.getElementById( "financing_result" ).style.display = "none";
        document.getElementById( "financing_query" ).style.display = "flex";

        let form = document.getElementById( "financing_form" );
        form.querySelector("input[name=\"value\"]").value = ""; 
        form.querySelector("input[name=\"initial_value\"]").value = "";
        form.querySelector("input[name=\"deadline\"]").value = "";
    }
    
    toogle_search_overlay( );

    document.getElementById( "financing_screen" ).style.display = display;
}

function toogle_search_overlay( )
{
    let display = "block";
    if (document.getElementById( "search_container" ).style.display == "block")
    {
        display = "none";
        document.getElementById( "search_result" ).style.display = "none";

        let form = document.getElementById( "search_form" );
        form.querySelector("#type-select").value = "";
        form.querySelector("input[name=\"area\"]").value = ""; 
        form.querySelector("input[name=\"age\"]").value = ""; 
        form.querySelector("#garage-checkbox").checked = false; 
        form.querySelector("#public-transports-checkbox").checked = false;
    }

    document.getElementById( "search_container" ).style.display = display;
}

function toogle_overlays()
{
    if ( document.getElementById( 'financing_screen' ).style.display == 'block' )
        return toogle_financing_screen();
    
    let display = "block";
    if (document.getElementById( "overlay" ).style.display == "block")
    {
        display = "none";
        document.getElementById( "overlay-btn" ).innerText = "Abrir Menu";
    }
    else
    {
        document.getElementById( "overlay-btn" ).innerText = "Fechar Menu";

        if ( toggled_elem )
            toggled_elem.classList.remove("open");
        
        toggled_elem = null;        
    }

    document.getElementById( "overlay" ).style.display = display;

    return toogle_search_overlay();
}

document.addEventListener('click', ( event ) => {
    if ( document.getElementById( "search_container" ).contains( event.target ) ||
         document.getElementById( "financing_screen" ).contains( event.target ) ||
         document.getElementById( "overlay-btn" ).contains( event.target ) ||
         document.getElementById( "overlay" ).style.display != "block" )
        return;
    
    if ( document.getElementById( "search_container" ).style.display == "block" )
    {
        toogle_search_overlay( );

        document.getElementById( "overlay-btn" ).innerText = "Abrir Menu";
        document.getElementById( "overlay" ).style.display = "none";
    }
    else if ( document.getElementById( "financing_result" ).style.display == "flex" )
    {
        document.getElementById( "financing_result" ).style.display = "none";
        document.getElementById( "financing_query" ).style.display = "flex";

        let form = document.getElementById( "financing_form" );
        form.querySelector("input[name=\"value\"]").value = ""; 
        form.querySelector("input[name=\"initial_value\"]").value = "";
        form.querySelector("input[name=\"deadline\"]").value = "";
    }
    else
        toogle_financing_screen( );
});

set_dropdown_listeners();