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
			this.zoom = new Zoom([image.view, path.view, position.view]);
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
				this.zoom.setBounds([0, 0, width, height]);
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
			 * @param point :Object The position point
			 *     @property x :Number The x coordinate
			 *     @property y :Number The y coordinate
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
			},

			/**
			 * Sets the zoom of the canvases with respect to the given scale center
			 *
			 * @param zoom :Number The zoom value
			 * @param center :Object The scale center
			 *     @property x :Number The x coordinate
			 *     @property y :Number The y coordinate
			 */
			zoom: function(zoom, center) {
				this.zoom.zoom(zoom, center);
			},

			/**
			 * Scrolls the view of the canvases of a given vector
			 *
			 * @param vector :Object The translation vector
			 *     @property x :Number The x coordinate
			 *     @property y :Number The y coordinate
			 */
			scroll: function(vector) {
				this.zoom.scroll(vector);
			},

			/**
			 * Calculates the absolute position respect to the drawings coordinates
			 * of a given point on the surface of the canvas
			 *
			 * @param point :Object The point to transform
			 *     @property x :Number The x coordinate
			 *     @property y :Number The y coordinate
			 *
			 * @return :Object The transformed point
			 *     @property x :Number The x coordinate
			 *     @property y :Number The y coordinate
			 */
			absolutePoint: function(point) {
				var res = this.zoom.absolutePoint(point)
				return { x: res.x, y: res.y };
			}
		}
	});

});
