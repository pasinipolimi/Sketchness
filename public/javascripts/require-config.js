require.config({
	baseUrl: "assets/javascripts",
	paths: {
		"jquery": "lib/jquery",
        "jquery-ui": "lib/jquery-ui",
		"i18n": "lib/jquery.i18n.properties",
		"mousewheel": "lib/jquery.mousewheel",
		"jscrollpane": "lib/jquery.jscrollpane",
		"popup": "lib/popUp",
		"paper": "lib/paper",
		"howler": "lib/howler",
		"StateMachine": "lib/state-machine",
		"nouislider": "lib/nouislider",
		"link": "lib/Link",
		"spectrum": "lib/spectrum"
	},
	shim: {
		"jquery": {
			exports: "$"
		},
		"i18n": {
			deps: ["jquery"],
			exports: "$.i18n"
		},
		"jscrollpane": {
			deps: ["jquery", "mousewheel"],
			exports: "$.fn.jScrollPane"
		},
		"popup": {
			deps: ["jquery"],
			exports: "$.fn.popup"
		},
		"paper": {
			exports: "paper"
		},
		"StateMachine": {
			exports: "StateMachine"
		},
		"link": {
			deps: ["jquery"],
			exports: "$.Link"
		},
		"nouislider": {
			deps: ["jquery", "link"],
			exports: "$.fn.noUiSlider"
		},
		"spectrum": {
		    deps: ["jquery"],
			exports: "$.fn.spectrum"
		}
	}
});
