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

			this.tools = {
				pen: {
					blendMode: "source-over"
				},
				eraser: {
					blendMode: "destination-out"
				}
			}

			this.tool = "pen";
			this.color = new paper.Color();
			this.size = 5;

			this.path = null;
			this.group = new paper.Group();

			this.closed = false;
		},

		_proto: {
			/**
			 * Switch the mode between pen and eraser
			 *
			 * @param tool :String("pen"|"eraser") The tool type
			 */
			setTool: function(tool) {
				if(tool in this.tools) {
					this.tool = tool;
				}
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
			 * (If the paths were hidden before, deletes
			 * them cleaning the canvas)
			 */
			begin: function() {
				this.project.activate();

				if(!this.group.visible) {
					this.group.removeChildren();
					this.group.setVisible(true);
				}

				this.path = new paper.Path({
					strokeColor: this.color,
					strokeWidth: this.size,
					blendMode: this.tools[this.tool].blendMode
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

					this.view.draw();
				}
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
			 * Hides the draws
			 */
			hide: function() {
				this.end();
				this.group.setVisible(false);

				this.view.draw();
			},

			/**
			 * Shows the paths if hidden
			 */
			show: function() {
				this.group.setVisible(true);
				this.view.draw();
			}
		}

	});

});
