define(["Class", "paper", "jquery"], function(Class, paper, $) {

	/**
	 * Handles the zoom of all the given overlapped canvases
	 */
	return new Class({
		_name: "Zoom",

		/**
		 * Constructor that takes the views to handle
		 *
		 * @param views :paper.Views The views to handle
		 */
		_init: function(views) {
			this.views = views;
			this.image = new paper.Rectangle(0,0,0,0);
			this.min = 1;
		},

		_proto: {
			/**
			 * Sets the bounds of current image to compute limits of zoom
			 * and set the zoom to fit the given rectangle
			 *
			 * @param image :paper.Rectangle The bounds of the image
			 */
			setBounds: function(image) {
				this.image = new paper.Rectangle(image);

				var ratio = this.views[0].size.divide(this.image.size);
				this.min = Math.min(1, ratio.width, ratio.height);

				this.zoom(this.min);
			},

			/**
			 * Sets the zoom of the canvases with respect to the given scale center
			 *
			 * The zoom value should be in the interval [min, max],
			 * where min (<= 1) is the value to fit the image in the view
			 * and max (= 1) is the value to see the image in its real dimensions
			 *
			 * @param zoom :Number The zoom value
			 * @param center :paper.Point The scale center
			 */
			zoom: function(zoom, center) {
				zoom = Math.max(Math.min(1, zoom), this.min);

				var matrix = this.fixMatrix(new paper.Matrix().scale(zoom / this.views[0].zoom, center));

				$.each(this.views, function(i, view) {
					view._transform(matrix);
					view._zoom = zoom;
				});
			},

			/**
			 * Change the zoom of a given scale factor, with respect to the scale center
			 *
			 * @see this.zoom
			 *
			 * @param scale :Number The scale factor
			 * @param center :paper.Point The scale center
			 */
			scale: function(scale, center) {
				this.zoom(this.views[0].zoom * scale, center);
			},

			/**
			 * Scrolls the view of the canvases of a given vector
			 *
			 * @param vector :paper.Point The translation vector
			 */
			scroll: function(vector) {
				vector = new paper.Point(vector).divide(this.views[0].zoom).negate();

				var matrix = this.fixMatrix(new paper.Matrix().translate(vector));

				$.each(this.views, function(i, view) {
					view._transform(matrix);
				});
			},

			/**
			 * Checks and fixes the issue of having the image out of the view bounds
			 *
			 * @param matrix :paper.Matrix The transformation matrix to fix
			 *
			 * @return :paper.Matrix A fixed new matrix
			 */
			fixMatrix: function(matrix) {
				matrix = new paper.Matrix(matrix);

				var box = this.views[0].bounds,
					image = this.image,
					vector = new paper.Point(0,0);

				box = new paper.Rectangle(
					matrix.inverseTransform(box.topLeft),
					matrix.inverseTransform(box.bottomRight)
				);

				if (image.width < box.width) {
					vector.x = box.center.x - image.center.x;
				} else if (image.left > box.left) {
					vector.x = box.left - image.left;
				} else if (image.right < box.right) {
					vector.x = box.right - image.right;
				}

				if (image.height < box.height) {
					vector.y = box.center.y - image.center.y;
				} else if (image.top > box.top) {
					vector.y = box.top - image.top;
				} else if (image.bottom < box.bottom) {
					vector.y = box.bottom - image.bottom;
				}

				return matrix.translate(vector);
			},

			/**
			 * Calculates the absolute position respect to the drawings coordinates
			 * of a given point on the surface of the canvas
			 *
			 * @param point :paper.Point The point to transform
			 *
			 * @return :paper.Point The transformed point
			 */
			absolutePoint: function(point) {
				return this.views[0].viewToProject(point);
			}
		}
	});
});
