/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function visualizzaImg(){
    var ids = $("selection").val();
    var ida = $("attivo").val();
    if(ida !== ""){
        $(ida).hide();
    }
    $(ids).show();
    $("attivo").val(ids);
}

function updateSize(evt){

}
