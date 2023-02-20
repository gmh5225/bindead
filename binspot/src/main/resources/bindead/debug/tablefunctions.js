    function elementHideShow(elementToHideOrShow) 
    {
        var el = document.getElementById(elementToHideOrShow);
        if (el.style.display == "block") {

            el.style.display = "none";
        }
        else 
        {
            el.style.display = "block";
        }
    }  
