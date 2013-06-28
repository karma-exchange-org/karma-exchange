// TODO(avaliani): Is there a way to add a namespace to this global?
var fbAppId;
if (document.location.hostname === "localhost") {
  fbAppId = '276423019167993';
} else if (document.location.hostname === "karmademo.dyndns.dk") {
  fbAppId = '1381630838720301';
} else {
  fbAppId = '571265879564450';
}
