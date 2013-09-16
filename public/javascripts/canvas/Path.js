define(["Class", "paper"], function(Class, paper) {

	/**
	 * Draw the path of the sketcher
	 */
	return new Class({
		_name: "Path",

		/**
		 * Given the ID or the HTML element of the canvas,
		 * setups paper project.
		 *
		 * @param canvas :String|DOMCanvas The canvas to use
		 */
		_init: function(canvas) {
			paper.setup(canvas);
			this.project = paper.project;
			this.view = this.project.view;

			this.eraser = false;
			this.color = new paper.Color();
			this.size = 5;

			this.path = null;
			this.group = new paper.Group();
		},

		_proto: {
			/**
			 * Switch the mode between pen and eraser
			 *
			 * @param eraser :Boolean True to activate eraser
			 */
			setEraser: function(eraser) {
				this.eraser = eraser;
			},

			/**
			 * Sets the color of the path
			 *
			 * @param color :paper.Color The color of the path
			 */
			setColor: function(color) {
				this.color = color;
			},

			/**
			 * Sets the size of the path
			 *
			 * @param size :Number The size of the path
			 */
			setSize: function(size) {
				this.size = size;
			},

			/**
			 * Begins a path with the given properties
			 */
			begin: function() {
				this.project.activate();

				this.path = new paper.Path({
					strokeColor: this.color,
					strokeWidth: this.size,
					blendMode: (this.eraser ? "destination-out" : "source-over")
				});

				this.group.addChild(this.path);
			},

			/**
			 * Adds a point at the end of current path
			 *
			 * @param point :paper.Point The point to add
			 */
			add: function(point) {
				if(this.path !== null) {
					this.path.add(point);
				}

				this.view.draw();
			},

			/**
			 * Ends a path and apply the simplification on it
			 */
			end: function() {
				if(this.path !== null) {
					this.path.simplify(10);
					this.path = null;
				}

				this.view.draw();
			},

			/**
			 * Clears the path canvas
			 */
			clear: function() {
				this.group.removeChildren();
				this.view.draw();
			}
		}

	});

});
