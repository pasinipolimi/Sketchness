define(["Class", "./Image", "./Path", "./Position"], function(Class, Image, Path, Position) {

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
			this.path = new Path(path);
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
			 * Sets the tools properties
			 *
			 * @param tool :Object The properties of the tool
			 *     @property tool :String("pen"|"eraser") The tool type
			 *     @property color :String The color of the tool
			 *     @property size :Number The size of the tool
			 */
			setTool: function(tool) {
				this.path.setTool(tool.tool);

				this.position.setColor(tool.color);
				this.path.setColor(tool.color);

				this.position.setSize(tool.size);
				this.path.setSize(tool.size);
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
			 * @param point :paper.Point The x y coordinates of the point
			 */
			setPoint: function(point) {
				this.position.draw(point);
				this.path.add(point);
			},

			/**
			 * Hides the position from the canvas.
			 */
			hidePosition: function() {
				this.position.hide();
			},

			/**
			 * Shows the position on the canvas.
			 */
			showPosition: function() {
				this.position.show();
			},

			/**
			 * Hides the draw from the canvas.
			 */
			hidePath: function() {
				this.path.hide();
			},

			/**
			 * Shows the draw on the canvas.
			 */
			showPath: function() {
				this.path.show();
			},

			/**
			 * Begin a new path
			 */
			beginPath: function() {
				this.path.begin();
			},

			/**
			 * Ends and simplify the path
			 */
			endPath: function() {
				this.path.end();
			}
		}
	});

});
