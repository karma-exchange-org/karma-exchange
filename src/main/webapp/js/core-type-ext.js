// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/findIndex
if (!Array.prototype.findIndexExt) {
  Object.defineProperty(Array.prototype, 'findIndexExt', {
    enumerable: false,
    configurable: true,
    writable: true,
    value: function(predicate) {
      if (this == null) {
        throw new TypeError('Array.prototype.findIndexExt called on null or undefined');
      }
      if (typeof predicate !== 'function') {
        throw new TypeError('predicate must be a function');
      }
      var list = Object(this);
      var length = list.length >>> 0;
      var thisArg = arguments[1];
      var value;

      for (var i = 0; i < length; i++) {
        if (i in list) {
          value = list[i];
          if (predicate.call(thisArg, value, i, list)) {
            return i;
          }
        }
      }
      return -1;
    }
  });
}

// TODO(avaliani): move this.
// My own addition
if (!Array.prototype.orderedInsertExt) {
  Object.defineProperty(Array.prototype, 'orderedInsertExt', {
    enumerable: false,
    configurable: true,
    writable: true,
    value: function(newEl, orderCb) {
      if (this == null) {
        throw new TypeError('Array.prototype.orderedInsert called on null or undefined');
      }
      if (typeof orderCb !== 'function') {
        throw new TypeError('orderCb must be a function');
      }
      var list = Object(this);
      var length = list.length >>> 0;

      var idx = 0;
      for (; idx < length; idx++) {
        var curEl = list[idx];
        if (orderCb.call(this, newEl, curEl) <= 0) {
          break;
        }
      }
      list.splice(idx, 0, newEl);
    }
  });
}
