/*
 * Copyright 2010 The Closure Compiler Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview Externs for jQuery Tooltip plugin 1.3
 * @see http://bassistance.de/jquery-plugins/jquery-plugin-tooltip/
 * @externs
 */

/** @type {Object.<string, *>} */
jQuery.tooltip = {};

/** @type {boolean} */
jQuery.tooltip.blocked;

/** @type {jQuery.tooltip.settings} */
jQuery.tooltip.defaults;

/** @return {undefined} */
jQuery.tooltip.block = function() {};

/**
 * @private
 * @constructor
 */
jQuery.tooltip.settings = function() {};

/** @type {number} */
jQuery.tooltip.settings.prototype.delay;

/** @type {(string|number)} */
jQuery.tooltip.settings.prototype.fade;

/** @type {boolean} */
jQuery.tooltip.settings.prototype.fixPNG;

/** @type {boolean} */
jQuery.tooltip.settings.prototype.track;

/** @type {boolean} */
jQuery.tooltip.settings.prototype.showURL;

/** @type {String} */
jQuery.tooltip.settings.prototype.extraClass;

/** @type {number} */
jQuery.tooltip.settings.prototype.top;

/** @type {number} */
jQuery.tooltip.settings.prototype.left;

/** @type {string} */
jQuery.tooltip.settings.prototype.id;

/**
 * @return {(string|Element)}
 * @this {Element}
 */
jQuery.tooltip.settings.prototype.bodyHandler = function() {};

/** @type {boolean} */
jQuery.tooltip.settings.prototype.showBody;

/**
 * @param {jQuery.tooltip.settings=} settings
 * @return {jQuery}
 */
jQuery.prototype.tooltip = function(settings) {};