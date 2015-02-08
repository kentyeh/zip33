function doOnload(){
    document.forms[0].elements["addr"].focus();
}
function doSubmit(form) {
    if (!form.elements["addr"].value) {
        form.elements["addr"].focus();
        alert("請輸入地址！");
        return false;
    } else
        return true;
}