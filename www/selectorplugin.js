var PLUGIN_NAME = 'SelectorCordovaPlugin';
var SelectorCordovaPlugin = function() {};

function Create2DArray(rows) {
	var arr = [];

	for (var i=0;i<rows;i++) {
		arr[i] = [];
	}

	return arr;
}

SelectorCordovaPlugin.prototype.showSelector = function(options, success_callback, error_callback, change_callback) {
	options || (options = {});

  	var scope = options.scope || null,
        displayList = Create2DArray(options.items.length),
      	config = {
			title: options.title || ' ',
			displayKey: options.displayKey || 'description',
			items: options.items || {},
			displayItems: displayList,
			defaultItems: options.defaultItems || {},
			theme: options.theme || 'light',
			wrapWheelText: options.wrapWheelText || false,
			positiveButtonText: options.positiveButtonText || 'Done',
			negativeButtonText: options.negativeButtonText || 'Cancel',
			fontSize: options.fontSize || 16,
			changeEvent: (typeof change_callback === 'function' ? true : false)
		};

	for(i in config.items) {
		for(k in config.items[i]) {
			for(n in config.items[i][k]) {
				displayList[i][n] = config.items[i][k][n][config.displayKey];
			}
		}
	}

	var _success_callback = function() {
		var type = arguments[0].type;

		arguments[0] = arguments[0].selection;

		if(type === 'change') {
			if(typeof change_callback == 'function') {
				change_callback.apply(scope, arguments);
			}
		} else if(type === 'confirm') {
			if(typeof success_callback == 'function') {
				success_callback.apply(scope, arguments);
			}
		}
	};

	var _error_callback = function() {
		if(typeof error_callback == 'function') {
			error_callback.apply(scope, arguments);
		}
	}

	return cordova.exec(_success_callback, _error_callback, PLUGIN_NAME, 'showSelector', [config]);
};

//heavily modified for Ionic2
SelectorCordovaPlugin.prototype.show = function(options, success_callback, error_callback) {
    options || (options = {});
    
	var scope = options.scope || null,
		displayList = Create2DArray(options.items.length),
		defaultItemsList = {},
		config = {
			title: options.title || ' ',
			displayKey: options.displayKey || 'description',
			items: options.items || {},
			displayItems: displayList,
			defaultItems: defaultItemsList,
			theme: options.theme || 'light',
			wrapWheelText: options.wrapWheelText || false,
			positiveButtonText: options.positiveButtonText || 'Done',
			negativeButtonText: options.negativeButtonText || 'Cancel',
			fontSize: options.fontSize || 16,
			changeEvent: (typeof change_callback === 'function' ? true : false)
		};

    for(i in options.items) {
		for(k in options.items[i]) {
			displayList[i][k] = options.items[i][k][config.displayKey];
		}
    }

    if(options.defaultItems != null && options.defaultItems.length > 0) {
		for(i in options.defaultItems) {
			defaultItemsList[options.defaultItems[i].index] = options.defaultItems[i].value;
		}
    }

    var _success_callback = function() {
		var type = arguments[0].type;

		arguments[0] = arguments[0].selection;

		if(type === 'change') {
			if(typeof change_callback == 'function') {
				change_callback.apply(scope, arguments);
			}
		} else if(type === 'confirm') {
			if(typeof success_callback == 'function') {
				success_callback.apply(scope, arguments);
			}
		}
	};

    var _error_callback = function() {
        if(typeof error_callback == 'function') {
            error_callback.apply(scope, arguments);
        }
    }

    return cordova.exec(_success_callback, _error_callback, PLUGIN_NAME, 'showSelector', [config]);
};

SelectorCordovaPlugin.prototype.hideSelector = function(success_callback, error_callback) {
  return cordova.exec(success_callback, error_callback, PLUGIN_NAME, 'hideSelector', []);
};

SelectorCordovaPlugin.prototype.updateItems = function(options) {
	options || (options = {});

  	var displayList = Create2DArray(options.items.length),
      	config = {
			displayItems: displayList,
			defaultItems: options.defaultItems || {},
		};

	for(i in options.items) {
		for(k in options.items[i]) {
			for(n in options.items[i][k]) {
				displayList[i][n] = options.items[i][k][n][(options.displayKey || "description")];
			}
		}
	}

    return cordova.exec(function(){}, function(){}, PLUGIN_NAME, 'updateItems', [config]);
};

module.exports = new SelectorCordovaPlugin();