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
				position: this.view.center,
				visible: false
			});

			var that = this;
			this.raster.on("load", function() {
				that.fit();
				that.raster.setVisible(true);
				that.view.draw();

				// TODO: handle the load event externally
				$(that).trigger("load");
			});

			this.rectangle = new paper.Path.Rectangle({
				size: this.view.size,
				radius: 50
			});

			this.group = new paper.Group({
				children: [this.rectangle, this.raster],
				clipped: true
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
				this.imgsize = new paper.Size(size);
			},

			/**
			 * Hides the image from the canvas.
			 */
			hide: function() {
				this.raster.setVisible(false);
				this.view.draw();
			},

			/**
			 * Set the size of the image to fit
			 * the view area keeping ratio.
			 */
			fit: function() {
				if (this.imgsize.width > this.view.size.width ||
					this.imgsize.height > this.view.size.height) {

					this.raster.fitBounds(this.view.bounds);
				}
			}
		}
	});

});
