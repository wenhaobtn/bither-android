/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.util;

import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionConfidence.ConfidenceType;

import net.bither.BitherApplication;

public class ConfidenceUtil {
	public static int getDepthInChain(TransactionConfidence confidence) {
		if (confidence == null) {
			return 0;
		}
		if (confidence.getConfidenceType() != ConfidenceType.BUILDING) {
			return 0;
		}
		return Math.max(
				0,
				BitherApplication.ChainHeight
						- confidence.getAppearedAtChainHeight() + 1);
	}
}
