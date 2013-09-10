define(["Class", "./Image"], function(Class, Image) {

	/**
	 * General proxy handler for canvases.
	 */
	return new Class({
		_name: "Painter",

		/**
		 * Setups the specific handlers given
		 * the ID or the HTML element of each canvas.
		 *
		 * @param image :String|DOMCanvas The canvas to use for images
		 * @param path :String|DOMCanvas The canvas to use for paths
		 * @param position :String|DOMCanvas The canvas to use for positions
		 */
		_init: function(image, path, position) {
			this.image = new Image(image);
		},

		_proto: {
			/**
			 * Shows the given image.
			 *
			 * @param image :String The url of the img
			 * @param width :Number The original width of img
			 * @param height :Number The original height of img
			 */
			showImage: function(image, width, height) {
				this.image.show(image, [width, height]);
			},

			/**
			 * Hide the image
			 */
			hideImage: function() {
				this.image.hide();
			}
		}
	});

});
