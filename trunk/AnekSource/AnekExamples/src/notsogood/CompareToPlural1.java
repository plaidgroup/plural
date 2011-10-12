package notsogood;

import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.Perm;
import edu.cmu.cs.plural.annot.Pure;
import edu.cmu.cs.plural.annot.States;
import edu.cmu.cs.plural.annot.TrueIndicates;

/**
 * This class and CompareToPlural2 will ideally allow us
 * to compare the performance of Plural's inference to
 * Anek's inference. The idea is just to make a program
 * with a lot of hairy control flow and then inline it
 * for comparison to Plural.
 * 
 * @author Nels E. Beckman
 *
 */
// RUN PLURAL ON ME
public class CompareToPlural1 {

	@Perm(requires="full(#0)",ensures="full(#0)")
	void reallyComplicatedMethod(Iterater i) {
		while(i.hasNext()) {
			while(i.hasNext()) {
				while(i.hasNext()) {
					while(i.hasNext()) {
						while(i.hasNext()) {
							while(i.hasNext()) {
								while(i.hasNext()) {
									while(i.hasNext()) {
										while(i.hasNext()) {
											while(i.hasNext()) {
												while(i.hasNext()) {
													while(i.hasNext()) {
														while(i.hasNext()) {
															while(i.hasNext()) {
																while(i.hasNext()) {
																	while(i.hasNext()) {
																		while(i.hasNext()) {
																			while(i.hasNext()) {
																				while(i.hasNext()) {
																					while(i.hasNext()) {
																						while(i.hasNext()) {
																							while(i.hasNext()) {
																								while(i.hasNext()) {
																									while(i.hasNext()) {
																										while(i.hasNext()) {
																											while(i.hasNext()) {
																												while(i.hasNext()) {
																													while(i.hasNext()) {
																														while(i.hasNext()) {
																															while(i.hasNext()) {
																																while(i.hasNext()) {
																																	while(i.hasNext()) {
																																		while(i.hasNext()) {
																																			while(i.hasNext()) {
																																				while(i.hasNext()) {
																																					while(i.hasNext()) {
																																						i.next();
																																					}
																																					while(i.hasNext()) {
																																						i.next();
																																					}
																																				}
																																				while(i.hasNext()) {
																																					i.next();
																																				}
																																			}
																																			while(i.hasNext()) {
																																				i.next();
																																			}
																																		}
																																		while(i.hasNext()) {
																																			i.next();
																																		}
																																	}
																																	while(i.hasNext()) {
																																		i.next();
																																	}
																																}
																																while(i.hasNext()) {
																																	i.next();
																																}
																															}
																															while(i.hasNext()) {
																																i.next();
																															}
																														}
																														while(i.hasNext()) {
																															i.next();
																														}
																													}
																													while(i.hasNext()) {
																														i.next();
																													}
																												}
																												while(i.hasNext()) {
																													i.next();
																												}
																											}
																											while(i.hasNext()) {
																												i.next();
																											}
																										}
																										while(i.hasNext()) {
																											i.next();
																										}
																									}
																									while(i.hasNext()) {
																										i.next();
																									}
																								}
																								while(i.hasNext()) {
																									i.next();
																								}
																							}
																							while(i.hasNext()) {
																								i.next();
																							}
																						}
																						while(i.hasNext()) {
																							i.next();
																						}
																					}
																					while(i.hasNext()) {
																						i.next();
																					}
																				}
																				while(i.hasNext()) {
																					i.next();
																				}
																			}
																			while(i.hasNext()) {
																				i.next();
																			}
																		}
																		while(i.hasNext()) {
																			i.next();
																		}
																	}
																	while(i.hasNext()) {
																		i.next();
																	}
																}
																while(i.hasNext()) {
																	i.next();
																}
															}
															while(i.hasNext()) {
																i.next();
															}
														}
														while(i.hasNext()) {
															i.next();
														}
													}
													while(i.hasNext()) {
														i.next();
													}
												}
												while(i.hasNext()) {
													i.next();
												}
											}
											while(i.hasNext()) {
												i.next();
											}
										}
										while(i.hasNext()) {
											i.next();
										}
									}
									while(i.hasNext()) {
										i.next();
									}
								}
								while(i.hasNext()) {
									i.next();
								}
							}
							while(i.hasNext()) {
								i.next();
							}
						}
						while(i.hasNext()) {
							i.next();
						}
					}
					while(i.hasNext()) {
						i.next();
					}
				}
				while(i.hasNext()) {
					i.next();
				}
			}
			while(i.hasNext()) {
				i.next();
			}
		}
		while(i.hasNext()) {
			i.next();
		}
	}	
}

@States({"HASNEXT"})
abstract class Iterater {
	@Perm(requires="pure(this)",ensures="pure(this)")
	//@TrueIndicates("HASNEXT")
	abstract boolean hasNext();
	
	@Perm(requires="full(this)",ensures="full(this)")
	abstract Object next();
}
