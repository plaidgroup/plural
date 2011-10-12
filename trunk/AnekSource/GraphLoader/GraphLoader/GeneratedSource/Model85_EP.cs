using System;
using MicrosoftResearch.Infer;
using MicrosoftResearch.Infer.Distributions;
using MicrosoftResearch.Infer.Collections;
using MicrosoftResearch.Infer.Factors;

namespace MicrosoftResearch.Infer.Models.User
{
	/// <summary>
	/// Class for performing inference in model 'Model' using algorithm 'ExpectationPropagation'.
	/// </summary>
	/// <remarks>
	/// The easiest way to use this class is to wrap an instance in a CompiledAlgorithm object and use
	/// the methods on CompiledAlgorithm to set parameters and execute inference.
	/// 
	/// If you instead wish to use this class directly, you must perform the following steps:
	/// 1) Create an instance of the class
	/// 2) Set the value of any externally-set fields e.g. data, priors
	/// 3) Call the Reset() method
	/// 4) Call the Initialise() method once
	/// 5) Call the Update() method multiple times  - each call performs one iteration of inference
	/// 6) Use the XXXMarginal() methods to retrieve posterior marginals for different variables.
	/// 
	/// Generated by Infer.NET 2.3 beta 4 at 19:13 on 21 May 2010.
	/// </remarks>
	public class Model85_EP : IIterativeProcess
	{
		#region Fields
		// Messages from uses of 'vbool332'
		public Bernoulli[] vbool332_uses_B;
		// The constant 'vBernoulli170'
		public Bernoulli vBernoulli170;
		// Message from definition of 'vbool332'
		public Bernoulli vbool332_F;
		// Message to marginal of 'vbool332'
		public Bernoulli vbool332_marginal_B;
		#endregion

		#region Methods
		/// <summary>
		/// Configures constant values that will not change during the lifetime of the class.
		/// </summary>
		/// <remarks>
		/// This method should be called once only after the class is instantiated.  In future, it will likely become
		/// the class constructor.
		/// </remarks>
		public void Reset()
		{
			// Create array for 'vbool332_uses' backwards messages.
			this.vbool332_uses_B = new Bernoulli[0];
			this.vBernoulli170 = new Bernoulli(0.01);
			this.vbool332_F = ArrayHelper.MakeUniform<Bernoulli>(vBernoulli170);
			// Message to 'vbool332' from Random factor
			this.vbool332_F = UnaryOp<bool>.RandomAverageConditional<Bernoulli>(this.vBernoulli170);
		}

		/// <summary>
		/// Creates message arrays and initialises their values ready for inference to be performed.
		/// </summary>
		/// <remarks>
		/// This method should be called once each time inference is performed. Since the initialisation
		/// procedure normally dependson external values such as priors and array sizes, all external
		/// values must be set before calling this method.
		/// 
		/// As well as initialising message arrays, this method also performs any message passing that
		/// the scheduler determines need only be carried out once.
		/// </remarks>
		public void Initialise()
		{
			this.vbool332_marginal_B = ArrayHelper.MakeUniform<Bernoulli>(vBernoulli170);
			// Message to 'vbool332_marginal' from UsesEqualDef factor
			this.vbool332_marginal_B = UsesEqualDefOp.MarginalAverageConditional<Bernoulli>(this.vbool332_uses_B, this.vbool332_F, this.vbool332_marginal_B);
		}

		/// <summary>
		/// Performs one iteration of inference.
		/// </summary>
		/// <remarks>
		/// This method should be called multiple times, after calling Initialise(), in order to perform
		/// multiple iterations of message passing.  You can call methods to retrieve posterior marginals
		/// at any time - the returned marginal will be the estimated marginal given the current state of
		/// the message passing algorithm.  This can be useful for monitoring convergence of the algorithm.
		/// 
		/// Where the scheduler has determined inference can be performed without iteration, this method
		/// does nothing.
		/// </remarks>
		public void Update()
		{
		}

		/// <summary>
		/// Returns the marginal distribution for 'vbool332' given by the current state of the
		/// message passing algorithm.
		/// </summary>
		/// <returns>The marginal distribution</returns>
		public Bernoulli Vbool332Marginal()
		{
			return this.vbool332_marginal_B;
		}

		#endregion

	}

}
