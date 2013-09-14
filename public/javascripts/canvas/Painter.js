define(["Class", "./Image", "./Position"], function(Class, Image, Position) {

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
			this.position = new Position(position);
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
			 * Hides the image
			 */
			hideImage: function() {
				this.image.hide();
			},

			/**
			 * Sets the color of the tool
			 *
			 * @param color :String The color of the tool
			 */
			setColor: function(color) {
				this.position.setColor(color);
			},

			/**
			 * Set the tool size
			 *
			 * @param size :Number The size of the tool
			 */
			setSize: function(size) {
				this.position.setSize(size);
			},

			/**
			 * Set the name label for the cursor
			 *
			 * @param name :String The name of the sketcher
			 */
			setName: function(name) {
				this.position.setText(name);
			},

			/**
			 * Set the current position point
			 * for position and path
			 *
			 * @param x :Number The x position
			 * @param y :Number The y position
			 */
			setPoint: function(x, y) {
				this.position.draw([x, y]);
			},

			/**
			 * Hides the position from the canvas.
			 */
			hidePosition: function() {
				this.position.hide();
			}
		}
	});

});
