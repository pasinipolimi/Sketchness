define(["Class", "paper"], function(Class, paper) {

	/**
	 * Show the sketcher cursor current position.
	 */
	return new Class({
		_name: "Position",

		/**
		 * Given the ID or the HTML element of the canvas,
		 * setups paper project, tool circle and name flag.
		 *
		 * @param canvas :String|DOMCanvas The canvas to use
		 */
		_init: function(canvas) {
			paper.setup(canvas);
			this.project = paper.project;
			this.view = this.project.view;

			this.bounds = new paper.Rectangle();
			this.color = new paper.Color();
			this.text = "";

			this.circle = new paper.Path.Circle({
				center: [0, 0],
    			radius: 5,
    			strokeColor: 'red'
			});
			this.pointText = new paper.PointText({
				font: "monospace",
				fontSize: 9
			});

			this.group = new paper.Group({
				children: [this.circle, this.pointText],
				visible: true
			});

			this.changed = false;
		},

		_proto: {
			/**
			 * Sets the size of the tool.
			 *
			 * @param size :Number The tool size
			 */
			setSize: function(size) {
				this.bounds.setSize(new paper.Size(size, size));
				this.setRadius(this.circle,size/2);
				this.changed = true;
			},


			/**
			 * Get the radius of a shape.
			 *
			 * @param path :paper.Path the path from which retrieve the radius
			 */
			getRadius: function(path) {
				return path.bounds.width / 2 + path.strokeWidth / 2;
				// or return path.strokeBounds.width / 2; 
			},

			/**
			 * Set the radius of a shape.
			 *
			 * @param path :paper.Path the path from which retrieve the radius, radius :Number
			 */
			setRadius: function(path, radius) {
				// figure out what the new radius should be without the stroke
				var newRadiusWithoutStroke = radius - path.strokeWidth / 2;
				// figure out what the current radius is without the stroke 
				var oldRadiusWithoutStroke = path.bounds.width / 2;
				path.scale(newRadiusWithoutStroke / oldRadiusWithoutStroke);
			},

			/**
			 * Sets the color of the tool.
			 *
			 * @param color :paper.Color The tool color
			 */
			setColor: function(color) {
				this.color = color;
				this.changed = true;
			},

			/**
			 * Sets the text label with the current sketcher name.
			 *
			 * @param text :String The name of the sketcher
			 */
			setText: function(text) {
				this.text = text;
				this.changed = true;
			},

			/**
			 * Draws the cursor position on the canvas.
			 *
			 * @param position :paper.Point The current position
			 */
			draw: function(position) {
				if(this.changed) {
					//this.circle = new paper.Path.Circle(new paper.Point(position.x, position.y), 50);
					this.circle.setStrokeColor(this.color);
					this.pointText.setFillColor(this.color);

					this.bounds.setCenter(position);

					this.circle.setPosition(position.x,position.y);

					this.pointText.setContent(this.text);
					this.pointText.setPosition(this.bounds.topCenter.subtract([0, this.pointText.bounds.height]));

					this.changed = false;
				} else {
					var delta = this.bounds.center.subtract(position).multiply(-1);
					this.group.translate(delta);
					this.bounds.setCenter(position);
				}

				this.group.setVisible(true);
				this.view.draw();
			},

			/**
			 * Hides the position from the canvas.
			 */
			hide: function() {
				this.group.setVisible(false);
				this.view.draw();
			},

			/**
			 * Shows the position if hidden
			 */
			show: function() {
				this.group.setVisible(true);
				this.view.draw();
			}
		}
	});

});
