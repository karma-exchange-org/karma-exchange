// TODO(avaliani): Is there a way to add a namespace to this global?
var fbAppId;
if (document.location.hostname === "localhost" ) {
    fbAppId = '276423019167993';
}
else if ( (document.location.hostname === "karmademo.dyndns.dk") ||
          (document.location.hostname === "karmademo.dyndns.org") ) {
    fbAppId = '1381630838720301';
}
else if ( /^kex-latest[-\w]*.appspot.com$/.test(document.location.hostname) ||
          (document.location.hostname === "karmademo.appspot.com") ) {
    fbAppId = '166052360247234';
}
else {
    fbAppId = '571265879564450';
}
