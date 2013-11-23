define(["Class", "paper", "jquery"], function(Class, paper, $) {

	/**
	 * Shows the task image in the relative canvas
	 */
	return new Class({
		_name: "Image",

		/**
		 * Given the ID or the HTML element of the canvas,
		 * setups paper project, raster and cut mask.
		 *
		 * @param canvas :String|DOMCanvas The canvas to use
		 */
		_init: function(canvas) {
			paper.setup(canvas);
			this.project = paper.project;
			this.view = this.project.view;

			this.raster = new paper.Raster({
				visible: false
			});

			var that = this;
			this.raster.on("load", function() {
				that.raster.setVisible(true);

				// TODO: handle the load event externally
				$(that).trigger("load");
			});
		},

		_proto: {

			/**
			 * Draws on the canvas an image rescaled
			 * to fit the view size.
			 *
			 * @param image :String The url of the img
			 * @param size :paper.Size The original size of img
			 */
			show: function(image, size) {
				this.raster.setSource(image);
				this.raster.setPosition(new paper.Size(size).divide(2));
			},

			/**
			 * Hides the image from the canvas.
			 */
			hide: function() {
				this.raster.setVisible(false);
				this.view.draw();
			}
		}
	});

});
